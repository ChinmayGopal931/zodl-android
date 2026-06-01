package co.electriccoin.zcash.di

import co.electriccoin.zcash.ui.screen.chat.common.ChatBootstrap
import co.electriccoin.zcash.ui.screen.chat.repository.ChatConversationsRepository
import co.electriccoin.zcash.ui.screen.chat.repository.ChatConversationsRepositoryImpl
import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepository
import co.electriccoin.zcash.ui.screen.chat.repository.ChatModerationRepositoryImpl
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.justzappit.zappmessaging.ZappMessagingSDK

val zappMessagingModule =
    module {
        singleOf(::ZappMessagingSDK)
        singleOf(::ChatModerationRepositoryImpl) bind ChatModerationRepository::class
        singleOf(::ChatConversationsRepositoryImpl) bind ChatConversationsRepository::class
        single { ChatBootstrap(androidApplication(), get(), get(), get()) }
    }
