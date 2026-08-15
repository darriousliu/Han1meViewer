package com.yenaly.han1meviewer.logic.model

import androidx.compose.ui.text.intl.Locale
import com.yenaly.han1meviewer.util.CHINESE
import com.yenaly.han1meviewer.util.ENGLISH
import com.yenaly.han1meviewer.util.JAPANESE
import com.yenaly.han1meviewer.util.LanguageHelper
import com.yenaly.han1meviewer.util.Parcelable
import com.yenaly.han1meviewer.util.Parcelize
import com.yenaly.han1meviewer.util.SIMPLIFIED_CHINESE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("EqualsOrHashCode")
@Serializable
@Parcelize
data class ReportReason(
    @SerialName("lang")
    val lang: Language? = null,
    @SerialName("reason_key")
    val reasonKey: String? = null
) : Parcelable {
    @Serializable
    @Parcelize
    data class Language(
        @SerialName("zh-rCN")
        val zhrCN: String? = null,
        @SerialName("zh-rTW")
        val zhrTW: String? = null,
        @SerialName("en")
        val en: String? = null,
        @SerialName("ja")
        val ja: String? = null,
    ) : Parcelable

    override fun hashCode(): Int = reasonKey?.hashCode() ?: 0

    val value: String
        get() {
            if (lang == null) return reasonKey.orEmpty()

            val pl = LanguageHelper.preferredLanguage
            return when (pl.language) {
                Locale.CHINESE.language -> when (pl.region) {
                    Locale.SIMPLIFIED_CHINESE.region -> lang.zhrCN
                    else -> lang.zhrTW
                }

                Locale.ENGLISH.language -> lang.en
                Locale.JAPANESE.language -> lang.ja
                else -> lang.zhrTW
            } ?: lang.zhrTW.orEmpty()
        }
}
