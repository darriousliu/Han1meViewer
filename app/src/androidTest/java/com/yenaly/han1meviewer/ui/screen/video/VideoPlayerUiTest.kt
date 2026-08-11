package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.playback.compose.VideoKeyframeUiState
import com.yenaly.han1meviewer.playback.compose.VideoPlayerActions
import com.yenaly.han1meviewer.playback.compose.VideoPlayerUiState
import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import com.yenaly.han1meviewer.playback.model.QualityVariant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPlayerUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun playAndLockControlsEmitSemanticActions() {
        val actions = RecordingActions()
        setPlayerContent(actions = actions)

        composeRule.onAllNodesWithContentDescription(
            context.getString(R.string.play_pause)
        )[0].performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.use_lock_screen)
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(1, actions.togglePlayPauseCalls)
            assertEquals(true, actions.lastLocked)
        }
    }

    @Test
    fun capabilitiesHideSpeedAndAnime4kControls() {
        setPlayerContent(
            state = playerState(
                capabilities = PlaybackCapabilities(
                    supportsQualitySelection = false,
                    supportsPlaybackSpeed = false,
                    supportsSuperResolution = false,
                )
            )
        )

        composeRule.onNodeWithText("1.0x").assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.anime_4k).replace('\n', ' '))
            .assertDoesNotExist()
    }

    @Test
    fun playbackErrorShowsMessageAndRetries() {
        val actions = RecordingActions()
        setPlayerContent(
            state = playerState(
                playback = playbackState.copy(
                    phase = PlaybackPhase.Error,
                    errorMessage = "network error",
                )
            ),
            actions = actions,
        )

        composeRule.onNodeWithText("network error").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()

        composeRule.runOnIdle { assertEquals(1, actions.retryCalls) }
    }

    @Test
    fun endedStateReplaysFromBeginning() {
        val actions = RecordingActions()
        setPlayerContent(
            state = playerState(playback = playbackState.copy(phase = PlaybackPhase.Ended)),
            actions = actions,
        )

        composeRule.onNodeWithText(context.getString(R.string.start_from_beginning)).performClick()

        composeRule.runOnIdle { assertEquals(1, actions.replayCalls) }
    }

    @Test
    fun qualityAndSpeedMenusEmitSelections() {
        val actions = RecordingActions()
        setPlayerContent(actions = actions)

        composeRule.onNodeWithText("1080P").performClick()
        composeRule.onNodeWithText("720P").performClick()
        composeRule.onNodeWithText("1.0x").performClick()
        composeRule.onNodeWithText("1.5x").performClick()

        composeRule.runOnIdle {
            assertEquals("720P", actions.selectedQuality)
            assertEquals(1.5f, checkNotNull(actions.selectedSpeed), 0f)
        }
    }

    @Test
    fun keyframePanelEmitsSelection() {
        val actions = RecordingActions()
        setPlayerContent(
            state = playerState(
                keyframesEnabled = true,
                keyframePanelVisible = true,
                keyframes = listOf(VideoKeyframeUiState(12_000L, "key moment")),
            ),
            actions = actions,
        )

        composeRule.onNodeWithText("key moment").assertIsDisplayed()
        composeRule.onNodeWithText("00:12").performClick()

        composeRule.runOnIdle { assertEquals(12_000L, actions.selectedKeyframe) }
    }

    private fun setPlayerContent(
        state: VideoPlayerUiState = playerState(),
        actions: RecordingActions = RecordingActions(),
    ) {
        composeRule.setContent {
            MaterialTheme {
                VideoPlayerUi(
                    state = state,
                    actions = actions,
                    surface = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun playerState(
        playback: PlaybackState = playbackState,
        capabilities: PlaybackCapabilities = PlaybackCapabilities(
            supportsSuperResolution = true,
        ),
        keyframesEnabled: Boolean = false,
        keyframePanelVisible: Boolean = false,
        keyframes: List<VideoKeyframeUiState> = emptyList(),
    ) = VideoPlayerUiState(
        playback = playback,
        capabilities = capabilities,
        longPressSpeedMultiplier = 2.5f,
        seekGestureSensitivity = 5,
        keyframesEnabled = keyframesEnabled,
        keyframePanelVisible = keyframePanelVisible,
        keyframes = keyframes,
    )

    private val playbackState = PlaybackState(
        source = Source,
        phase = PlaybackPhase.Ready,
        durationMs = 60_000L,
        selectedQualityId = "1080P",
    )

    private class RecordingActions : VideoPlayerActions {
        var togglePlayPauseCalls = 0
        var retryCalls = 0
        var replayCalls = 0
        var lastLocked: Boolean? = null
        var selectedQuality: String? = null
        var selectedSpeed: Float? = null
        var selectedKeyframe: Long? = null

        override fun onBack() = Unit
        override fun onHome() = Unit
        override fun onTogglePlayPause() {
            togglePlayPauseCalls++
        }
        override fun onRetry() {
            retryCalls++
        }
        override fun onReplay() {
            replayCalls++
        }
        override fun onSeekTo(positionMs: Long) = Unit
        override fun onSelectQuality(qualityId: String) {
            selectedQuality = qualityId
        }
        override fun onSetSpeed(speed: Float) {
            selectedSpeed = speed
        }
        override fun onSetSuperResolution(index: Int) = Unit
        override fun onOpenKeyframes() = Unit
        override fun onDismissKeyframes() = Unit
        override fun onSelectKeyframe(positionMs: Long) {
            selectedKeyframe = positionMs
        }
        override fun onAddKeyframe() = Unit
        override fun onToggleFullscreen() = Unit
        override fun onControlsVisibilityChanged(visible: Boolean) = Unit
        override fun onLockChanged(locked: Boolean) {
            lastLocked = locked
        }
        override fun onBrightnessChanged(fraction: Float) = Unit
        override fun onVolumeChanged(fraction: Float) = Unit
        override fun onRestartFromBeginning() = Unit
    }

    private companion object {
        val Source = PlaybackSource(
            id = "test",
            title = "Test video",
            qualities = listOf(
                QualityVariant("1080P", uri = "https://example.com/1080.mp4"),
                QualityVariant("720P", uri = "https://example.com/720.mp4"),
            ),
        )
    }
}
