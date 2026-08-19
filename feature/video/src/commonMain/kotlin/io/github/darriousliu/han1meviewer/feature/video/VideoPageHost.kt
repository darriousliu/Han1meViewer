package io.github.darriousliu.han1meviewer.feature.video

interface VideoPageHost {
    fun showCommentBadge(count: Int)
    fun shouldEnterPip(): Boolean
    fun enterPipMode()
    fun onPipModeChanged(isInPip: Boolean)
    fun togglePlayPause()
}
