package co.electriccoin.zcash.ui.screen.swap.upi.bridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.spackle.Twig
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.provider.BridgeTerminallyFailedException
import co.electriccoin.zcash.ui.common.provider.OfframpTopUpCheckpoint
import co.electriccoin.zcash.ui.common.provider.OfframpTopUpCheckpointStorageProvider
import co.electriccoin.zcash.ui.common.provider.OfframpTopUpPreview
import co.electriccoin.zcash.ui.common.provider.StoreCorruptedException
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.component.zapp.ZappConfirmationState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.ellipsizeMiddle
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpStep
import co.electriccoin.zcash.ui.screen.swap.upi.progress.UpiOfframpStepStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.justzappit.evm.rpc.BaseRpcClient
import xyz.justzappit.evm.types.Address
import xyz.justzappit.offramp.account.SmartOfframpAccountProvider
import xyz.justzappit.offramp.config.P2pNetworkConfig
import xyz.justzappit.offramp.orchestrator.BridgeToBaseStatus
import xyz.justzappit.offramp.orchestrator.OfframpDriver
import xyz.justzappit.offramp.p2p.CurrencyCode
import xyz.justzappit.offramp.p2p.Usdc6
import xyz.justzappit.offramp.p2p.getPriceConfig
import xyz.justzappit.offramp.p2p.getUsdcBalance
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@Suppress("TooManyFunctions")
internal class BridgeToBaseVM(
    private val args: BridgeToBaseArgs,
    private val navigationRouter: NavigationRouter,
    private val rpc: BaseRpcClient,
    private val network: P2pNetworkConfig,
    private val accountProvider: SmartOfframpAccountProvider,
    private val orchestrator: OfframpDriver,
    private val topUpPreview: OfframpTopUpPreview,
    private val checkpointStorage: OfframpTopUpCheckpointStorageProvider,
) : ViewModel() {
    private sealed interface Phase {
        data object Input : Phase

        data class Bridging(val depositAddress: String?) : Phase

        data class Complete(val addedAmount: Usdc6, val baseBalance: Usdc6) : Phase

        // [resumeHandle] non-null when the same 1-Click bridge can be re-polled instead of re-quoted.
        data class Failed(val message: StringResource, val resumeHandle: String?) : Phase
    }

    // Data resolved once when the screen opens (account balance, merchant availability, sell rate, bridge
    // ETA). Bundled so the state combine stays at three flows.
    private data class Priming(
        val baseBalance: Usdc6? = null,
        val availability: BridgeAvailabilityHint? = null,
        val sellRate: BigDecimal = FALLBACK_RATE,
        val etaSeconds: Int? = null,
    )

    private val amount = MutableStateFlow(initialAmount())
    private val phase = MutableStateFlow<Phase>(Phase.Input)
    private val priming = MutableStateFlow(Priming())

    // Confirmation shown when the user backs out mid-bridge, so an in-flight bridge isn't silently
    // abandoned. Surfaced separately from [state]. Null = hidden.
    private val leaveConfirmation = MutableStateFlow<ZappConfirmationState?>(null)
    val leaveConfirmationState: StateFlow<ZappConfirmationState?> = leaveConfirmation.asStateFlow()

    private var smartAccountAddress: Address? = null
    private var bridgeJob: Job? = null

    init {
        viewModelScope.launch { resolveAndPrime() }
        viewModelScope.launch { resumeIfInFlight() }
        // Re-probe the amount-sensitive hints (merchant availability, ETA) when the entered amount
        // changes; collectLatest cancels the prior probe so rapid typing doesn't pile up RPCs.
        viewModelScope.launch {
            amount
                .map { parseUsdc(it) }
                .distinctUntilChanged()
                .collectLatest {
                    if (phase.value is Phase.Input) {
                        refreshAvailability()
                        refreshEta()
                    }
                }
        }
    }

    val state: StateFlow<BridgeToBaseState> =
        combine(amount, phase, priming) { amt, currentPhase, prime ->
            buildState(amt, currentPhase, prime)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = buildState(amount.value, Phase.Input, Priming()),
        )

    private suspend fun resolveAndPrime() {
        val account =
            runCatching { accountProvider.resolve().address }
                .onFailure { Twig.warn(it) { "BridgeToBaseVM: smart account resolve failed" } }
                .getOrNull() ?: return
        smartAccountAddress = account
        refreshBalance()
        refreshRate()
        refreshAvailability()
        refreshEta()
    }

    private suspend fun refreshBalance() {
        val account = smartAccountAddress ?: return
        runCatching { rpc.getUsdcBalance(network.usdcAddress, account) }
            .onSuccess { fetched -> priming.update { it.copy(baseBalance = fetched) } }
            .onFailure { Twig.warn(it) { "BridgeToBaseVM: getUsdcBalance failed" } }
    }

    private suspend fun refreshRate() {
        runCatching { rpc.getPriceConfig(network.diamondAddress, CURRENCY).sellPriceAsRate() }
            .onSuccess { rate -> priming.update { it.copy(sellRate = rate) } }
            .onFailure { Twig.warn(it) { "BridgeToBaseVM: getPriceConfig failed" } }
    }

    // Best-effort hint: warn (non-blocking) if no merchant currently has liquidity, since the bridged
    // USDC persists on Base and stays usable for a later payment regardless.
    private suspend fun refreshAvailability() {
        val probe = enteredUsdc() ?: Usdc6.ofWhole(PROBE_USDC)
        val available = orchestrator.isMerchantAvailable(probe, CURRENCY)
        priming.update {
            it.copy(
                availability =
                    BridgeAvailabilityHint(
                        text =
                            if (available) {
                                stringRes(R.string.bridge_to_base_merchants_available)
                            } else {
                                stringRes(R.string.bridge_to_base_merchants_unavailable)
                            },
                        isWarning = !available,
                    ),
            )
        }
    }

    private suspend fun refreshEta() {
        val account = smartAccountAddress ?: return
        val probe = enteredUsdc() ?: Usdc6.ofWhole(PROBE_USDC)
        val seconds = topUpPreview.estimatedDurationSeconds(account, probe)
        priming.update { it.copy(etaSeconds = seconds) }
    }

    private suspend fun resumeIfInFlight() {
        // A read failure here just skips auto-resume; onAddFunds re-checks before starting, so a missed
        // resume can't cause a double-send.
        val existing = (readCheckpoint() as? CheckpointRead.Ok)?.checkpoint ?: return
        val addUsdc = existing.addUsdc() ?: return
        amount.update { amountField(addUsdc) }
        startBridge(addUsdc, resumeHandle = existing.bridgeDepositAddress)
    }

    private sealed interface CheckpointRead {
        data class Ok(val checkpoint: OfframpTopUpCheckpoint?) : CheckpointRead

        data object Failed : CheckpointRead
    }

    private suspend fun readCheckpoint(): CheckpointRead =
        try {
            CheckpointRead.Ok(checkpointStorage.get())
        } catch (e: StoreCorruptedException) {
            Twig.warn(e) { "BridgeToBaseVM: corrupted top-up checkpoint, discarding" }
            checkpointStorage.clear()
            CheckpointRead.Ok(null)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Twig.warn(e) { "BridgeToBaseVM: top-up checkpoint read failed" }
            CheckpointRead.Failed
        }

    private fun startBridge(addUsdc: Usdc6, resumeHandle: String?) {
        if (bridgeJob?.isActive == true) return
        phase.update { Phase.Bridging(depositAddress = resumeHandle) }
        bridgeJob =
            viewModelScope.launch {
                orchestrator.bridgeToBase(addUsdc, resumeHandle).collect { status ->
                    Twig.info { "BridgeToBase status=${status::class.simpleName}" }
                    onBridgeStatus(addUsdc, status)
                }
            }
    }

    private suspend fun onBridgeStatus(addUsdc: Usdc6, status: BridgeToBaseStatus) {
        when (status) {
            BridgeToBaseStatus.Idle -> Unit

            is BridgeToBaseStatus.Bridging -> {
                status.depositAddress?.let { addr ->
                    checkpointStorage.store(
                        OfframpTopUpCheckpoint(
                            bridgeDepositAddress = addr,
                            addUsdcMicroDecimal = addUsdc.micros.toString(),
                            createdAtMillis = System.currentTimeMillis(),
                        ),
                    )
                }
                phase.update { Phase.Bridging(depositAddress = status.depositAddress) }
            }

            is BridgeToBaseStatus.Complete -> {
                checkpointStorage.clear()
                priming.update { it.copy(baseBalance = status.baseBalance) }
                phase.update { Phase.Complete(addedAmount = status.addedAmount, baseBalance = status.baseBalance) }
            }

            is BridgeToBaseStatus.Failed -> {
                // Terminal 1-Click failures clear the checkpoint (re-polling a dead bridge loops
                // forever); a transient failure keeps it so "Try again" re-polls the same deposit
                // address instead of opening a second bridge and double-sending the user's ZEC.
                val terminal = status.cause is BridgeTerminallyFailedException
                val resumeHandle = if (terminal) null else status.depositAddress
                if (resumeHandle == null) checkpointStorage.clear()
                phase.update {
                    Phase.Failed(
                        message =
                            when {
                                terminal -> stringRes(R.string.bridge_to_base_failed_terminal)
                                // A bridge was opened, so retry only re-polls — no risk of re-sending ZEC.
                                resumeHandle != null -> stringRes(R.string.bridge_to_base_failed_retry)
                                // Failed before any ZEC moved (quote/network); retry starts fresh.
                                else -> stringRes(R.string.bridge_to_base_failed_generic)
                            },
                        resumeHandle = resumeHandle,
                    )
                }
            }
        }
    }

    private fun buildState(
        amt: NumberTextFieldInnerState,
        currentPhase: Phase,
        prime: Priming,
    ): BridgeToBaseState {
        val isInput = currentPhase is Phase.Input
        val entered = parseUsdc(amt)
        return BridgeToBaseState(
            amountInput = NumberTextFieldState(innerState = amt, onValueChange = ::onAmountChange),
            baseBalanceText =
                prime.baseBalance?.let {
                    stringRes(R.string.upi_offramp_base_balance_label, it.toDisplayString(stripTrailingZeros = true))
                },
            inrValueText = (entered?.let { inrValueText(it, prime.sellRate) }).takeIf { isInput },
            etaText = etaText(prime.etaSeconds).takeIf { isInput },
            availabilityHint = prime.availability.takeIf { isInput },
            explainer = stringRes(R.string.bridge_to_base_explainer),
            errorText = (currentPhase as? Phase.Failed)?.message,
            steps = stepsFor(currentPhase),
            isInputVisible = isInput,
            primaryButton = primaryButtonFor(currentPhase, entered),
            onBack = ::onBackRequested,
        )
    }

    // Confirm before abandoning an in-flight bridge; otherwise just leave.
    private fun onBackRequested() {
        if (phase.value is Phase.Bridging) {
            leaveConfirmation.update { leaveConfirmationSheet() }
        } else {
            navigationRouter.back()
        }
    }

    private fun leaveConfirmationSheet() =
        ZappConfirmationState(
            title = stringRes(R.string.bridge_to_base_leave_title),
            message = stringRes(R.string.bridge_to_base_leave_message),
            primaryButton =
                ButtonState(
                    text = stringRes(R.string.bridge_to_base_leave_confirm),
                    onClick = {
                        leaveConfirmation.update { null }
                        navigationRouter.back()
                    },
                ),
            secondaryButton =
                ButtonState(
                    text = stringRes(R.string.bridge_to_base_leave_stay),
                    onClick = { leaveConfirmation.update { null } },
                ),
            onBack = { leaveConfirmation.update { null } },
        )

    private fun inrValueText(usdc: Usdc6, sellRate: BigDecimal): StringResource {
        val inr = usdc.whole.multiply(sellRate).setScale(INR_DISPLAY_SCALE, RoundingMode.FLOOR)
        return stringRes(R.string.bridge_to_base_inr_value, inr.stripTrailingZeros().toPlainString())
    }

    private fun etaText(seconds: Int?): StringResource {
        if (seconds == null || seconds <= 0) return stringRes(R.string.bridge_to_base_eta_fallback)
        val minutes = ((seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE).coerceAtLeast(1)
        return stringRes(R.string.bridge_to_base_eta, minutes)
    }

    private fun primaryButtonFor(currentPhase: Phase, entered: Usdc6?): ButtonState =
        when (currentPhase) {
            Phase.Input ->
                ButtonState(
                    text = stringRes(R.string.bridge_to_base_add_button),
                    isEnabled = entered != null,
                    onClick = ::onAddFunds,
                )

            is Phase.Bridging ->
                ButtonState(
                    text = stringRes(R.string.bridge_to_base_bridging_button),
                    isEnabled = false,
                    onClick = {},
                )

            is Phase.Complete ->
                ButtonState(
                    text = stringRes(R.string.bridge_to_base_pay_button),
                    onClick = { navigationRouter.back() },
                )

            is Phase.Failed ->
                ButtonState(
                    text = stringRes(R.string.bridge_to_base_try_again_button),
                    onClick = ::onTryAgain,
                )
        }

    private fun stepsFor(currentPhase: Phase): List<UpiOfframpStep> =
        when (currentPhase) {
            Phase.Input -> emptyList()

            is Phase.Bridging ->
                listOf(
                    UpiOfframpStep(
                        label = stringRes(R.string.bridge_to_base_step_bridging),
                        status = UpiOfframpStepStatus.InProgress,
                        detailLines =
                            currentPhase.depositAddress?.let {
                                listOf(
                                    stringRes(
                                        R.string.upi_offramp_detail_deposit_addr,
                                        it.ellipsizeMiddle(DEPOSIT_ELLIPSIS_PREFIX, DEPOSIT_ELLIPSIS_SUFFIX),
                                    ),
                                )
                            }.orEmpty(),
                    ),
                    UpiOfframpStep(
                        label = stringRes(R.string.bridge_to_base_step_arrived),
                        status = UpiOfframpStepStatus.Pending,
                    ),
                )

            is Phase.Complete ->
                listOf(
                    UpiOfframpStep(stringRes(R.string.bridge_to_base_step_bridging), UpiOfframpStepStatus.Completed),
                    UpiOfframpStep(stringRes(R.string.bridge_to_base_step_arrived), UpiOfframpStepStatus.Completed),
                )

            is Phase.Failed ->
                listOf(
                    UpiOfframpStep(stringRes(R.string.bridge_to_base_step_bridging), UpiOfframpStepStatus.Failed),
                )
        }

    private fun onAmountChange(next: NumberTextFieldInnerState) {
        amount.update { next }
    }

    private fun onAddFunds() {
        val usdc = enteredUsdc() ?: return
        viewModelScope.launch {
            // Never open a fresh bridge while a persisted one exists — a tap during the init checkpoint
            // read (before resumeIfInFlight starts its job) would otherwise open a second bridge and
            // double-send ZEC. On a read failure, refuse to start rather than risk it.
            when (val read = readCheckpoint()) {
                is CheckpointRead.Failed -> failGeneric()
                is CheckpointRead.Ok -> {
                    val existing = read.checkpoint
                    if (existing == null) {
                        startBridge(usdc, resumeHandle = null)
                    } else {
                        startBridge(existing.addUsdc() ?: usdc, resumeHandle = existing.bridgeDepositAddress)
                    }
                }
            }
        }
    }

    private fun onTryAgain() {
        val handle = (phase.value as? Phase.Failed)?.resumeHandle
        val usdc = enteredUsdc() ?: run {
            phase.update { Phase.Input }
            return
        }
        if (handle != null) {
            startBridge(usdc, resumeHandle = handle)
        } else {
            phase.update { Phase.Input }
        }
    }

    private fun failGeneric() {
        phase.update {
            Phase.Failed(stringRes(R.string.bridge_to_base_failed_generic), resumeHandle = null)
        }
    }

    private fun enteredUsdc(): Usdc6? = parseUsdc(amount.value)

    private fun parseUsdc(state: NumberTextFieldInnerState): Usdc6? =
        state.amount
            ?.takeIf { it > BigDecimal.ZERO }
            ?.setScale(USDC_INPUT_SCALE, RoundingMode.FLOOR)
            ?.takeIf { it > BigDecimal.ZERO }
            ?.let { Usdc6.ofWhole(it) }
            ?.takeIf { it > Usdc6.ZERO }

    private fun OfframpTopUpCheckpoint.addUsdc(): Usdc6? =
        runCatching { Usdc6(BigInteger(addUsdcMicroDecimal)) }.getOrNull()?.takeIf { it > Usdc6.ZERO }

    private fun amountField(usdc: Usdc6): NumberTextFieldInnerState =
        NumberTextFieldInnerState.fromAmount(usdc.whole.setScale(DISPLAY_SCALE, RoundingMode.FLOOR))

    private fun initialAmount(): NumberTextFieldInnerState =
        args.prefillUsdcMicro
            ?.let { runCatching { Usdc6(BigInteger(it)) }.getOrNull() }
            ?.takeIf { it > Usdc6.ZERO }
            ?.let { amountField(it) }
            ?: NumberTextFieldInnerState()

    companion object {
        private val CURRENCY = CurrencyCode.Inr

        // Used for the INR estimate until getPriceConfig returns; ₹85/USDC is the p2p.me historical default.
        private val FALLBACK_RATE: BigDecimal = BigDecimal("85")

        // Nominal amount for the merchant-availability and ETA probes when no amount is entered yet.
        private val PROBE_USDC: BigDecimal = BigDecimal("5")

        private const val USDC_INPUT_SCALE = 6
        private const val DISPLAY_SCALE = 2
        private const val INR_DISPLAY_SCALE = 2
        private const val SECONDS_PER_MINUTE = 60
        private const val DEPOSIT_ELLIPSIS_PREFIX = 10
        private const val DEPOSIT_ELLIPSIS_SUFFIX = 6
    }
}
