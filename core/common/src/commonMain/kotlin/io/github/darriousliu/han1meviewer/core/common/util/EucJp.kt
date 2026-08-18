package io.github.darriousliu.han1meviewer.core.common.util

/**
 * 按 EUC-JP 解码字节。
 *
 * getchu 的页面是 EUC-JP 编码，Ktor 的 `bodyAsText()` 按 UTF-8 解出来全是乱码，
 * 所以要拿原始字节自己解。
 *
 * 之所以是 expect/actual 而不是直接找个通用 API：**EUC-JP 是真正的平台差异**。
 * JVM 有 `Charset.forName("EUC-JP")`，而 Kotlin/Native 上 Ktor 的
 * `io.ktor.utils.io.charsets` 只支持 UTF-8，其它编码会直接抛；
 * Foundation 那边则有现成的 `NSJapaneseEUCStringEncoding`。
 *
 * 解不出来时返回空串（而不是抛），和调用方「解析失败就当空页面」的处理方式一致。
 */
expect fun ByteArray.decodeEucJp(): String
