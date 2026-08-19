package io.github.darriousliu.han1meviewer.core.navigation

/*
 * 屏幕之间的回传结果，走 nav3 的 `ResultEventBus`（androidx.navigation3.runtime.result）。
 *
 * 发送/接收都发生在 **route 层**（`MainNavDisplay`），屏幕本身仍然只收普通回调、
 * 不认识导航——和项目既有的「屏幕层副作用由 route 执行」保持一致。
 *
 * ⚠️ 结果走 Channel，**不跨配置变更/进程死亡存活**。
 */

/**
 * 头像裁剪页 → 账号页，直接传裁好的 JPEG 字节。
 *
 * 不用 data class：`ByteArray` 的 `equals` 是引用语义，data class 生成的
 * `equals`/`hashCode` 会有误导性，而这里根本不需要比较。
 */
class AvatarCropped(val jpeg: ByteArray)

/**
 * 登录成功。WebView 登录 / 账密登录 / 手动贴 cookie 三条路共用一个结果类型，
 * 由首页消费去刷新。
 */
data object LoginSucceeded

/**
 * 请求首页刷新（账号页改完头像等场景）。由首页消费；发送时首页在栈下层，
 * 结果会缓冲到返回首页时才生效——与「回到首页看到最新状态」的需求一致。
 */
data object HomeRefreshRequested
