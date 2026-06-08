package co.electriccoin.zcash.ui.screen.restore.tor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.SeedPhrase
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.repository.WalletRepository
import co.electriccoin.zcash.ui.common.usecase.RestoreWalletUseCase
import co.electriccoin.zcash.ui.common.usecase.ShowErrorUseCase
import co.electriccoin.zcash.ui.common.viewmodel.SecretState
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.CheckboxState
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.tabs.TabsArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RestoreTorVM(
    private val args: RestoreTorArgs,
    private val navigationRouter: NavigationRouter,
    private val restoreWallet: RestoreWalletUseCase,
    private val walletRepository: WalletRepository,
    private val showError: ShowErrorUseCase,
) : ViewModel() {
    private val isChecked = MutableStateFlow(false)

    val state: StateFlow<RestoreTorState> =
        isChecked
            .map {
                createState(it)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = createState(isChecked.value)
            )

    private fun createState(isChecked: Boolean): RestoreTorState =
        RestoreTorState(
            checkbox =
                CheckboxState(
                    title = stringRes(R.string.restore_tor_checkbox_title),
                    subtitle = stringRes(R.string.restore_tor_checkbox_subtitle),
                    isChecked = isChecked,
                    onClick = { this.isChecked.update { !it } }
                ),
            primary =
                ButtonState(
                    text = stringRes(R.string.restore_bd_restore_btn),
                    onClick = { onRestoreWalletClick(isChecked) },
                ),
            secondary =
                ButtonState(
                    text = stringRes(co.electriccoin.zcash.ui.design.R.string.general_cancel),
                    onClick = ::onBack,
                ),
            onBack = ::onBack
        )

    private fun onBack() = navigationRouter.back()

    private fun onRestoreWalletClick(isChecked: Boolean) =
        viewModelScope.launch {
            restoreWallet(
                seedPhrase = SeedPhrase.new(args.seed.trim()),
                enableTor = isChecked,
                birthday = BlockHeight.new(args.blockHeight)
            )
            // WalletRepository.restoreWallet runs its work in an internal scope and
            // returns synchronously. Without this gate, the user is left on the Tor
            // dialog on top of WALLET_CHOICE in ZappOnboardingFlow; that step's
            // auto-advance is what actually steps the onboarding forward, so we have
            // to pop the restore sub-flow once the secret state has flipped.
            val didSucceed =
                combine(
                    walletRepository.secretState,
                    walletRepository.walletProvisioningError,
                ) { state, error ->
                    when {
                        error != null -> false
                        state == SecretState.READY -> true
                        else -> null
                    }
                }.filterNotNull().first()
            if (didSucceed) {
                navigationRouter.replaceAll(TabsArgs)
            } else {
                showError()
            }
        }
}
