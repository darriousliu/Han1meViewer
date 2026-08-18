package io.github.darriousliu.han1meviewer.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.module

/**
 * Koin 的唯一启动入口。三端各自的宿主在初始化时调它一次。
 *
 * [platformDeclaration] 给平台补自己的东西——Android 要在这里传
 * `androidContext(...)`，其它端目前不需要。
 */
fun initKoin(platformDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        platformDeclaration()
        modules(AppModule().module)
    }
}
