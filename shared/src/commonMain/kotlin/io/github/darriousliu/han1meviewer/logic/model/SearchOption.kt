package io.github.darriousliu.han1meviewer.logic.model

import androidx.compose.ui.text.intl.Locale
import io.github.darriousliu.han1meviewer.util.CHINESE
import io.github.darriousliu.han1meviewer.util.ENGLISH
import io.github.darriousliu.han1meviewer.util.JAPANESE
import io.github.darriousliu.han1meviewer.util.LanguageHelper
import io.github.darriousliu.han1meviewer.util.Parcelable
import io.github.darriousliu.han1meviewer.util.Parcelize
import io.github.darriousliu.han1meviewer.util.SIMPLIFIED_CHINESE
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.appearance_and_figure
import io.github.darriousliu.han1meviewer.core.resource.characteristics
import io.github.darriousliu.han1meviewer.core.resource.relationship
import io.github.darriousliu.han1meviewer.core.resource.sex_position
import io.github.darriousliu.han1meviewer.core.resource.story_location
import io.github.darriousliu.han1meviewer.core.resource.story_plot
import io.github.darriousliu.han1meviewer.core.resource.video_attr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Suppress("EqualsOrHashCode")
@Serializable
@Parcelize
data class SearchOption(
    @SerialName("lang")
    val lang: Language? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("search_key")
    val searchKey: String? = null,
) : Parcelable {

    companion object {
        fun Map<String, Set<SearchOption>>.flatten(): Set<String> = buildSet {
            values.forEach { options ->
                options.mapNotNullTo(this) { it.searchKey }
            }
        }

        operator fun Map<String, List<SearchOption>>.get(scopeNameRes: StringResource): List<SearchOption> {
            return when (scopeNameRes) {
                Res.string.video_attr -> this["video_attributes"].orEmpty()
                Res.string.relationship -> this["character_relationships"].orEmpty()
                Res.string.characteristics -> this["characteristics"].orEmpty()
                Res.string.appearance_and_figure -> this["appearance_and_figure"].orEmpty()
                Res.string.story_plot -> this["story_plot"].orEmpty()
                Res.string.story_location -> this["story_location"].orEmpty()
                Res.string.sex_position -> this["sex_positions"].orEmpty()
                else -> error("Unknown scope name res: $scopeNameRes")
            }
        }

        fun toScopeKey(raw: String): StringResource = when (raw) {
            "video_attributes" -> Res.string.video_attr
            "character_relationships" -> Res.string.relationship
            "characteristics" -> Res.string.characteristics
            "appearance_and_figure" -> Res.string.appearance_and_figure
            "story_plot" -> Res.string.story_plot
            "story_location" -> Res.string.story_location
            "sex_positions" -> Res.string.sex_position
            else -> error("Unknown scope name: $raw")
        }
    }

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

    override fun hashCode(): Int = searchKey.hashCode()

    val value: String
        get() = when {
            lang == null -> name.orEmpty()
            else -> LanguageHelper.preferredLanguage.let { pl ->
                when (pl.language) {
                    Locale.CHINESE.language -> when (pl.region) {
                        Locale.SIMPLIFIED_CHINESE.region -> lang.zhrCN
                        else -> lang.zhrTW
                    }

                    Locale.ENGLISH.language -> lang.en
                    Locale.JAPANESE.language -> lang.ja
                    else -> lang.zhrTW
                }
            } ?: lang.zhrTW.orEmpty()
        }
}