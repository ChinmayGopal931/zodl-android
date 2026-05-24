package co.electriccoin.zcash.ui.screen.offramp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.repository.ExchangeRateRepository
import co.electriccoin.zcash.ui.common.util.PeerXyzUtil
import co.electriccoin.zcash.ui.common.wallet.ExchangeRateState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.design.util.stringResByCurrencyNumber
import co.electriccoin.zcash.ui.screen.ExternalUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.MathContext

class OfframpVM(
    private val args: OfframpArgs,
    private val navigationRouter: NavigationRouter,
    exchangeRateRepository: ExchangeRateRepository,
) : ViewModel() {
    val amountZec: MutableStateFlow<String> =
        MutableStateFlow(
            args.prefillZatoshi?.let { Zatoshi(it).convertZatoshiToZec().toPlainString() }.orEmpty()
        )

    val state: StateFlow<OfframpState?> =
        combine(
            amountZec,
            exchangeRateRepository.state,
        ) { amount, rate ->
            createState(amount, rate)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = null
        )

    private fun createState(amount: String, rate: ExchangeRateState) =
        OfframpState(
            onBack = { navigationRouter.back() },
            amount = amount,
            fiatAmount = computeFiat(amount, rate),
            onAmountChange = { amountZec.tryEmit(it) },
            onContinue = ::onContinue,
            isContinueEnabled = amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true,
            attribution = stringRes(R.string.offramp_attribution),
        )

    private fun computeFiat(amount: String, rate: ExchangeRateState): StringResource? {
        val zec = amount.toBigDecimalOrNull() ?: return null
        if (zec <= BigDecimal.ZERO) return null
        val data = rate as? ExchangeRateState.Data ?: return null
        val conversion = data.currencyConversion ?: return null
        val fiatValue = zec.multiply(BigDecimal(conversion.priceOfZec), MathContext.DECIMAL128)
        val symbol = data.fiatCurrency.symbol
        return stringResByCurrencyNumber(amount = fiatValue, ticker = symbol)
    }

    private fun onContinue() {
        val zec = amountZec.value.ifBlank { return }
        navigationRouter.forward(ExternalUrl(url = PeerXyzUtil.getOfframpUrl(zec), branded = true))
    }
}
