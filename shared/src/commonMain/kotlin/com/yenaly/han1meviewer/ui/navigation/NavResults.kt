package com.yenaly.han1meviewer.ui.navigation

/*
 * 屏幕之间的回传结果，走 nav3 的 `ResultEventBus`（androidx.navigation3.runtime.result）。
 *
 * 这几条链路的来历都是「原来两个 Activity 之间 setResult / ActivityResultLauncher」：
 * Step 17 把 Activity 合并进导航图时，它们退化成了手写的状态提升 + 消费回调，
 * Step 19 换成 nav3 原生的结果总线。
 *
 * 发送/接收都发生在 **route 层**（`MainNavDisplay`），屏幕本身仍然只收普通回调、
 * 不认识导航——和项目既有的「屏幕层副作用由 route 执行」保持一致。
 *
 * ⚠️ 结果走 Channel，**不跨配置变更/进程死亡存活**。和换掉的那套 `remember`
 * 提升状态行为一致，不是回退。
 */

/**
 * 头像裁剪页 → 账号页。原 `AvatarCropActivity` 那条
 * 「裁完落 cacheDir → 回传路径 → 读回字节」链路的直系后代，现在直接传 JPEG 字节。
 *
 * 不用 data class：`ByteArray` 的 `equals` 是引用语义，data class 生成的
 * `equals`/`hashCode` 会有误导性，而这里根本不需要比较。
 */
class AvatarCropped(val jpeg: ByteArray)

/**
 * 登录成功。WebView 登录 / 账密登录 / 手动贴 cookie 三条路共用一个结果类型，
 * 由首页消费去刷新——对应原 `LoginActivity.setResult(RESULT_OK)` +
 * `MainActivity.loginDataLauncher` 里那次 `getHomePage()`。
 */
data object LoginSucceeded
