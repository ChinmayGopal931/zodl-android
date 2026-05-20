package co.electriccoin.zcash.di

import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.justzappit.zappmessaging.ZappMessagingSDK

val zappMessagingModule = module {
    single { ZappMessagingSDK() }
    single { ChatModerationRepository(androidContext()) }
    single { ChatBootstrap(androidApplication(), get(), get(), get()) }
}
