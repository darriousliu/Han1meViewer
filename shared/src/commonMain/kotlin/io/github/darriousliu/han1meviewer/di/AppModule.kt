package io.github.darriousliu.han1meviewer.di

import io.github.darriousliu.han1meviewer.feature.checkin.CheckinModule
import io.github.darriousliu.han1meviewer.feature.history.HistoryModule
import io.github.darriousliu.han1meviewer.feature.subscription.SubscriptionModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * commonMain 的定义总入口。
 *
 * `@ComponentScan` 扫的是整个 `io.github.darriousliu.han1meviewer` 包，所以新增
 * `@Single` / `@Factory` / `@KoinViewModel` 不用回来登记，写完注解就生效。
 *
 * 拆模块之后每个 core/feature 模块会有自己的 `@Module`，各扫各的包，
 * 这里改成汇总它们。
 */
@Module(includes = [CheckinModule::class, HistoryModule::class, SubscriptionModule::class])
@ComponentScan("io.github.darriousliu.han1meviewer")
class AppModule
