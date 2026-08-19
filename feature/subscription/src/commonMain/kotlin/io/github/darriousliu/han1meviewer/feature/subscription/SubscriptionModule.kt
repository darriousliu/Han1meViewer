package io.github.darriousliu.han1meviewer.feature.subscription

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** 本模块的 Koin 定义入口，:shared 的 AppModule includes 它。 */
@Module
@ComponentScan("io.github.darriousliu.han1meviewer.feature.subscription")
class SubscriptionModule
