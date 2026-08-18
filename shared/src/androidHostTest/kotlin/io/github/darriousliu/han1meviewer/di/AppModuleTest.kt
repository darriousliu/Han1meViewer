package io.github.darriousliu.han1meviewer.di

import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.ksp.generated.module
import org.koin.test.verify.verify

/**
 * 静态校验 Koin 的定义图：每个定义的构造参数都得能在图里找到。
 *
 * 缺依赖在运行时是「打开某个页面才崩」，这里能提前到构建期。
 * 每加一个 `@Single` / `@KoinViewModel` 都自动被这条覆盖，不用回来改测试。
 */
class AppModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `所有定义的依赖都能解析`() {
        AppModule().module.verify(
            // SavedStateHandle 不是模块里声明的，是 ViewModel 创建时由
            // CreationExtras 注入的，verify 看不到，得显式放行。
            extraTypes = listOf(SavedStateHandle::class)
        )
    }
}
