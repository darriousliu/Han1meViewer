package com.yenaly.han1meviewer.logic.model

import com.yenaly.han1meviewer.platform.platformServices
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("EqualsOrHashCode")
@Serializable
data class ReportReason(
    @SerialName("lang")
    val lang: Language? = null,
    @SerialName("reason_key")
    val reasonKey: String? = null
) {
    @Serializable
    data class Language(
        @SerialName("zh-rCN")
        val zhrCN: String? = null,
        @SerialName("zh-rTW")
        val zhrTW: String? = null,
        @SerialName("en")
        val en: String? = null,
        @SerialName("ja")
        val ja: String? = null,
    )

    override fun hashCode(): Int = reasonKey?.hashCode() ?: 0

    val value: String
        get() {
            if (lang == null) return reasonKey.orEmpty()

            val pl = platformServices().language.preferredLanguage()
            return when (pl.language) {
                "zh" -> when (pl.country) {
                    "CN" -> lang.zhrCN
                    else -> lang.zhrTW
                }
                "en" -> lang.en
                "ja" -> lang.ja
                else -> lang.zhrTW
            } ?: lang.zhrTW.orEmpty()
        }
}
