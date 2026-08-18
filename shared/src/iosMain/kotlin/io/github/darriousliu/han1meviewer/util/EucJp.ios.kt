package io.github.darriousliu.han1meviewer.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSJapaneseEUCStringEncoding
import platform.Foundation.NSString
import platform.Foundation.create

/**
 * 走 Foundation 的 `-[NSString initWithBytes:length:encoding:]`
 * （klib 里的签名是 `ObjCClassOf<T : NSString>.create(bytes, length, encoding): T?`）。
 *
 * 用这个三参重载而不是先包一层 `NSData`，少一次拷贝。它是 copy 语义（不是
 * `bytesNoCopy`），所以在 [usePinned] 的作用域里调用是安全的。
 *
 * ⚠️ 结果必须用 `toString()` 转，**不能 `as String`**：这个工厂函数的返回类型是泛型
 * `T?`（T : NSString），泛型位置上不会走 ObjC↔Kotlin 的 String 桥接，
 * 强转编译器会警告「this cast only succeeds when the expression is null」，
 * 运行时永远拿到 null。`toString()` 对 ObjC 对象走 `-description`，NSString 返回自身内容。
 *
 * `NSJapaneseEUCStringEncoding` 就是 EUC-JP。解码失败时 ObjC 返回 nil，这里落成空串。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun ByteArray.decodeEucJp(): String {
    if (isEmpty()) return ""
    return usePinned { pinned ->
        NSString.create(
            bytes = pinned.addressOf(0),
            length = size.toULong(),
            encoding = NSJapaneseEUCStringEncoding,
        )?.toString()
    } ?: ""
}
