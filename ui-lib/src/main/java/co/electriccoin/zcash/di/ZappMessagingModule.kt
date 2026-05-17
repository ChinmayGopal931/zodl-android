package co.electriccoin.zcash.di

import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepository
import co.electriccoin.zcash.ui.screen.chat.viewmodel.ChatViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import xyz.justzappit.zappmessaging.ZappMessagingSDK

val zappMessagingModule = module {
    single { ZappMessagingSDK() }
    single { ChatModerationRepository(androidContext()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
