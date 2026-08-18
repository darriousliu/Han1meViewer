package io.github.darriousliu.han1meviewer.logic.exception

import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.website_blocked_msg
import io.github.darriousliu.han1meviewer.core.resource.website_blocked_msg_2
import io.github.darriousliu.han1meviewer.core.resource.website_blocked_msg_3
import org.jetbrains.compose.resources.StringResource

/**
 * 检测到爬虫被封鎖
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/08/07 007 12:45
 */
open class CloudFlareBlockedException(
    override val messageRes: StringResource,
    devMessage: String = "blocked by Cloudflare",
) : RuntimeException(devMessage), LocalizedException {

    companion object {
        val localizedMessages = arrayOf(
            Res.string.website_blocked_msg,
            Res.string.website_blocked_msg_2,
            Res.string.website_blocked_msg_3,
        )
    }
}
