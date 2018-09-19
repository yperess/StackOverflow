package com.yperess.stackoverflow

import android.content.Context
import android.graphics.SurfaceTexture
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.video.VideoListener

/**
 *
 */
class MovieLiveWallpaperService : WallpaperService() {

    enum class Mode {
        FIT_CENTER,
        FIT_XY,
        CENTER_CROP
    }

    override fun onCreateEngine(): Engine = VideoLiveWallpaperEngine(Mode.FIT_CENTER)

    inner class VideoLiveWallpaperEngine(
            private val mode: Mode
    ) : WallpaperService.Engine(), VideoListener {

        private val bandwidthMeter = DefaultBandwidthMeter()
        private val exoMediaPlayer = initExoMediaPlayer()

        private val context: Context = this@MovieLiveWallpaperService

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            if (mode == Mode.FIT_CENTER) {
                // We need to somehow wrap the surface or set some scale factor on exo player here.
                // Most likely this will require creating a SurfaceTexture and attaching it to an
                // OpenGL context. Then for each frame, writing it to the original surface but with
                // an offset
                exoMediaPlayer.setVideoSurface(holder.surface)
            } else {
                exoMediaPlayer.setVideoSurfaceHolder(holder)
            }

            val videoUri = RawResourceDataSource.buildRawResourceUri(R.raw.small)
            val dataSourceFactory = DataSource.Factory { RawResourceDataSource(context) }
            val mediaSourceFactory = ExtractorMediaSource.Factory(dataSourceFactory)
            exoMediaPlayer.prepare(mediaSourceFactory.createMediaSource(videoUri))
        }

        override fun onDestroy() {
            exoMediaPlayer.release()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            exoMediaPlayer.playWhenReady = visible
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        // VideoListener

        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                pixelWidthHeightRatio: Float) {
            val frame = surfaceHolder.surfaceFrame
            val verticalRatio = frame.height().toFloat() / height
            val horizontalRatio = frame.width().toFloat() / width
            val minRatio = Math.min(verticalRatio, horizontalRatio)

            // This is the size we'd need to draw
            val newWidth = (width * minRatio).toInt()
            val newHeight = (height * minRatio).toInt()
        }

        override fun onRenderedFirstFrame() {}

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Private helper methods

        private fun initExoMediaPlayer(): SimpleExoPlayer {
            val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
            val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
            val player = ExoPlayerFactory.newSimpleInstance(this@MovieLiveWallpaperService,
                    trackSelector)
            player.playWhenReady = true
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.volume = 0f
            if (mode == Mode.CENTER_CROP) {
                player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            } else {
                player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
            if (mode == Mode.FIT_CENTER) {
                player.addVideoListener(this)
            }
            return player
        }
    }
}