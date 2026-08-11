package com.yenaly.han1meviewer.logic.network.cookie

import com.yenaly.han1meviewer.storage.AppStorage
import com.yenaly.han1meviewer.storage.StorageSchema
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * Process-wide cookie state for the main-site Ktor client.
 *
 * Response cookies intentionally remain memory-only. Login, Cloudflare and language values are
 * read from their MMKV-backed stores for every request so an in-process authentication update is
 * visible to the next request without rebuilding the client.
 */
object MainSiteCookieRepository {
    private const val USER_LANGUAGE_COOKIE = "user_lang"

    private val lock = reentrantLock()
    private val ephemeralByHost = mutableMapOf<String, List<CookiePair>>()

    /**
     * Builds the exact cookie header used by a normal main-site request.
     *
     * Android's Cloudflare retry path also uses this API after WebView updates the persisted
     * Cloudflare cookie, because the original request header has already been materialized.
     */
    fun cookieHeaderFor(host: String): String? = cookiePairsFor(host).let { cookies ->
        if (cookies.isEmpty()) null else {
            cookies.joinToString(separator = "; ", transform = CookiePair::render)
        }
    }

    /** Cookie pairs for platform engines that follow redirects below Ktor's response pipeline. */
    internal fun cookiePairsFor(host: String): List<CookiePair> = lock.withLock {
        val normalizedHost = host.normalizedCookieHost()
        buildList {
            addAll(ephemeralByHost[normalizedHost].orEmpty())
            addAll(loginCookieGroup())
            addAll(cloudflareCookieGroup())
        }
    }

    /** Clears only response cookies; persisted login and Cloudflare values remain untouched. */
    fun clearEphemeral() {
        lock.withLock { ephemeralByHost.clear() }
    }

    /** Replaces one host from one response as a single atomic operation. */
    internal fun replaceFromResponse(host: String, setCookieHeaders: List<String>) {
        val responseCookies = setCookieHeaders.mapNotNull(::parseResponseCookie)
        replaceParsedFromResponse(host, responseCookies)
    }

    /** Replaces one host from an engine-parsed response as a single atomic operation. */
    internal fun replaceParsedFromResponse(host: String, responseCookies: List<CookiePair>) {
        // OkHttp doesn't invoke CookieJar.saveFromResponse when no response cookie was parseable.
        if (responseCookies.isEmpty()) return

        lock.withLock {
            ephemeralByHost[host.normalizedCookieHost()] = buildList {
                addAll(responseCookies)
                // Preserve HCookieJar: response cookies are followed by a snapshot of login
                // cookies, including a freshly generated user_lang cookie, but not CF cookies.
                addAll(loginCookieGroup())
            }
        }
    }

    private fun loginCookieGroup(): List<CookiePair> = persistentCookieGroup(
        rawCookie = AppStorage.auth.value(StorageSchema.Auth.loginCookie),
    )

    private fun cloudflareCookieGroup(): List<CookiePair> = persistentCookieGroup(
        rawCookie = AppStorage.auth.value(StorageSchema.Auth.cloudflareCookie),
    )

    private fun persistentCookieGroup(rawCookie: String): List<CookiePair> = buildList {
        // Deliberately read this once per group. HCookieJar creates user_lang separately for the
        // login and Cloudflare raw strings, so request order includes both copies.
        add(
            CookiePair(
                name = USER_LANGUAGE_COOKIE,
                value = AppStorage.settings.value(StorageSchema.Settings.videoLanguage),
            )
        )
        addAll(parsePersistedRawCookies(rawCookie))
    }
}

internal data class CookiePair(
    val name: String,
    val value: String,
) {
    fun render(): String = "$name=$value"
}

/** Mirrors the legacy CookieString parser, including its missing-'=' value behavior. */
private fun parsePersistedRawCookies(rawCookie: String): List<CookiePair> = buildList {
    rawCookie.split(';').forEach { rawPart ->
        if (rawPart.isBlank()) return@forEach

        val name = rawPart.substringBefore('=').trim()
        val value = rawPart.substringAfter('=').trim()
        val cleanedName = name.filterCookieAscii()
        val cleanedValue = value.filterCookieAscii()
        if (
            cleanedName.isNotEmpty() &&
            cleanedName.trim() == cleanedName &&
            cleanedValue.trim() == cleanedValue
        ) {
            add(CookiePair(cleanedName, cleanedValue))
        }
    }
}

/** Reads only the leading name/value pair; attributes never affect HCookieJar request ordering. */
private fun parseResponseCookie(setCookieHeader: String): CookiePair? {
    val pair = setCookieHeader.substringBefore(';')
    val separator = pair.indexOf('=')
    if (separator < 0) return null

    // OkHttp's Set-Cookie parser trims only HTTP ASCII whitespace before validating ASCII.
    val name = pair.substring(0, separator).trimCookieAsciiWhitespace()
    val value = pair.substring(separator + 1).trimCookieAsciiWhitespace()
    if (name.isEmpty() || name.hasCookieControlOrNonAscii() || value.hasCookieControlOrNonAscii()) {
        return null
    }
    return CookiePair(name, value)
}

private fun String.filterCookieAscii(): String = filter { character ->
    character.code in 0x20..0x7E && character != '\n' && character != '\r'
}

private fun String.hasCookieControlOrNonAscii(): Boolean = any { character ->
    character.code < 0x20 || character.code >= 0x7F
}

private fun String.trimCookieAsciiWhitespace(): String = trim { character ->
    character == '\t' || character == '\n' || character == '\u000C' ||
        character == '\r' || character == ' '
}

private fun String.normalizedCookieHost(): String = lowercase()
