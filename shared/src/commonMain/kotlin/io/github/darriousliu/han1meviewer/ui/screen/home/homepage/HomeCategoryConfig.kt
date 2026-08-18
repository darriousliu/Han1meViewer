package io.github.darriousliu.han1meviewer.ui.screen.home.homepage

import io.github.darriousliu.han1meviewer.Preferences
import org.jetbrains.compose.resources.StringResource
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.ai_decensored
import io.github.darriousliu.han1meviewer.core.resource.ai_generated
import io.github.darriousliu.han1meviewer.core.resource.amateur_nomask
import io.github.darriousliu.han1meviewer.core.resource.animation_2_5d
import io.github.darriousliu.han1meviewer.core.resource.animation_2d
import io.github.darriousliu.han1meviewer.core.resource.category_3d_animation
import io.github.darriousliu.han1meviewer.core.resource.category_cosplay
import io.github.darriousliu.han1meviewer.core.resource.category_instant_noodle
import io.github.darriousliu.han1meviewer.core.resource.category_motion_anime
import io.github.darriousliu.han1meviewer.core.resource.china_av
import io.github.darriousliu.han1meviewer.core.resource.chinese_amateur
import io.github.darriousliu.han1meviewer.core.resource.chinese_subtitle
import io.github.darriousliu.han1meviewer.core.resource.hd_uncensored
import io.github.darriousliu.han1meviewer.core.resource.latest_av
import io.github.darriousliu.han1meviewer.core.resource.latest_hanime
import io.github.darriousliu.han1meviewer.core.resource.latest_release
import io.github.darriousliu.han1meviewer.core.resource.latest_upload
import io.github.darriousliu.han1meviewer.core.resource.mmd
import io.github.darriousliu.han1meviewer.core.resource.ranking_this_month
import io.github.darriousliu.han1meviewer.core.resource.ranking_today
import io.github.darriousliu.han1meviewer.core.resource.they_watched

const val HOME_CATEGORY_LATEST_HANIME = "latest_hanime"
const val HOME_CATEGORY_LATEST_RELEASE = "latest_release"
const val HOME_CATEGORY_LATEST_UPLOAD = "latest_upload"
const val HOME_CATEGORY_WATCHING_NOW = "watching_now"
const val HOME_CATEGORY_SHORT_EPISODE = "short_episode"
const val HOME_CATEGORY_MOTION_ANIME = "motion_anime"
const val HOME_CATEGORY_3D_CG = "3d_cg"
const val HOME_CATEGORY_2_5D = "2_5d"
const val HOME_CATEGORY_2D_ANIME = "2d_anime"
const val HOME_CATEGORY_AI_GENERATED = "ai_generated"
const val HOME_CATEGORY_MMD = "mmd"
const val HOME_CATEGORY_COSPLAY = "cosplay"

data class HomeCategoryPreferenceItem(
    val key: String,
    val normalTitleRes: StringResource,
    val avTitleRes: StringResource? = null,
)

val defaultHomeCategoryPreferenceItems = listOf(
    HomeCategoryPreferenceItem(HOME_CATEGORY_LATEST_HANIME, Res.string.latest_hanime, Res.string.latest_av),
    HomeCategoryPreferenceItem(HOME_CATEGORY_LATEST_RELEASE, Res.string.latest_release),
    HomeCategoryPreferenceItem(HOME_CATEGORY_LATEST_UPLOAD, Res.string.latest_upload),
    HomeCategoryPreferenceItem(HOME_CATEGORY_WATCHING_NOW, Res.string.they_watched),
    HomeCategoryPreferenceItem(HOME_CATEGORY_SHORT_EPISODE, Res.string.category_instant_noodle, Res.string.amateur_nomask),
    HomeCategoryPreferenceItem(HOME_CATEGORY_MOTION_ANIME, Res.string.category_motion_anime, Res.string.hd_uncensored),
    HomeCategoryPreferenceItem(HOME_CATEGORY_3D_CG, Res.string.category_3d_animation, Res.string.ai_decensored),
    HomeCategoryPreferenceItem(HOME_CATEGORY_2_5D, Res.string.animation_2_5d, Res.string.china_av),
    HomeCategoryPreferenceItem(HOME_CATEGORY_2D_ANIME, Res.string.animation_2d, Res.string.chinese_amateur),
    HomeCategoryPreferenceItem(HOME_CATEGORY_AI_GENERATED, Res.string.ai_generated, Res.string.chinese_subtitle),
    HomeCategoryPreferenceItem(HOME_CATEGORY_MMD, Res.string.mmd, Res.string.ranking_today),
    HomeCategoryPreferenceItem(HOME_CATEGORY_COSPLAY, Res.string.category_cosplay, Res.string.ranking_this_month),
)

val defaultHomeCategoryOrder: List<String>
    get() = defaultHomeCategoryPreferenceItems.map { it.key }

val homeCategoryOrder: List<String>
    get() = normalizeHomeCategoryKeys(
        Preferences.homeCategoryOrder
            .split(',')
            .filter { it.isNotBlank() }
    )

val hiddenHomeCategoryKeys: Set<String>
    get() = Preferences.homeCategoryHidden
        .split(',')
        .filter { it.isNotBlank() }
        .toSet()

fun saveHomeCategoryPreferences(order: List<String>, hiddenKeys: Set<String>) {
    Preferences.homeCategoryOrder = normalizeHomeCategoryKeys(order).joinToString(",")
    Preferences.homeCategoryHidden =
        hiddenKeys.filter { it in defaultHomeCategoryOrder }.joinToString(",")
}

private fun normalizeHomeCategoryKeys(keys: List<String>): List<String> {
    val defaults = defaultHomeCategoryOrder
    return keys.distinct().filter { it in defaults } + defaults.filterNot { it in keys }
}
