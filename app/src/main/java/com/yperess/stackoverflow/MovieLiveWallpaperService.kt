package com.yperess.stackoverflow

import android.content.SharedPreferences
import android.opengl.GLSurfaceView
import android.preference.PreferenceManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.video.VideoListener
import com.yperess.stackoverflow.opengl.BaseConfigChooser
import com.yperess.stackoverflow.opengl.DefaultContextFactory
import com.yperess.stackoverflow.opengl.DefaultWindowSurfaceFactory
import com.yperess.stackoverflow.opengl.GLThread
import timber.log.Timber

/**
 *
 */
class MovieLiveWallpaperService : WallpaperService() {

    enum class Mode {
        FIT_CENTER,
        FIT_START,
        FIT_END,
        FIT_XY,
        CENTER_CROP
    }

    private val mode: Mode
        get() = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("scale_type", Mode.CENTER_CROP.name).let(Mode::valueOf)

    override fun onCreateEngine(): Engine = GlEngine()

    protected fun initExoMediaPlayer(): SimpleExoPlayer {
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        player.playWhenReady = true
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 0f
        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        val videoUri = RawResourceDataSource.buildRawResourceUri(R.raw.small)
        val dataSourceFactory = DataSource.Factory { RawResourceDataSource(this) }
        val mediaSourceFactory = ExtractorMediaSource.Factory(dataSourceFactory)
        val mediaSource = mediaSourceFactory.createMediaSource(videoUri)
        player.prepare(LoopingMediaSource(mediaSource))
        return player
    }

    inner class GlEngine : WallpaperService.Engine(), VideoListener,
            SharedPreferences.OnSharedPreferenceChangeListener {

        private val exoMediaPlayer = initExoMediaPlayer()
        private val renderer = MovieWallpaperRenderer(exoMediaPlayer, mode,
                this@MovieLiveWallpaperService)

        private val glThread: GLThread

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Engine

        override fun onVisibilityChanged(visible: Boolean) {
            Timber.d("onVisibilityChanged(%s)", visible)
            exoMediaPlayer.playWhenReady = visible
            if (visible) onResume()
            else onPause()
        }

        @Synchronized
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            Timber.d("onSurfaceCreated(%s)", holder)
            glThread.surfaceCreated(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            Timber.d("onSurfaceChanged(%s, %d, %d, %d)", holder, format, width, height)
            glThread.onWindowResize(width, height)
            renderer.setSurfaceSize(width, height)
        }

        @Synchronized
        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            Timber.d("onSurfaceDestroyed(%s)", holder)
            glThread.requestExitAndWait()
        }

        override fun onDestroy() {
            Timber.d("onDestroy()")
            PreferenceManager.getDefaultSharedPreferences(this@MovieLiveWallpaperService)
                    .unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        // VideoListener

        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                pixelWidthHeightRatio: Float) {
            Timber.d("onVideoSizeChanged(%d, %d)", width, height)
            val frame = surfaceHolder.surfaceFrame
            val verticalRatio = frame.height().toFloat() / height
            val horizontalRatio = frame.width().toFloat() / width
            val minRatio = Math.min(verticalRatio, horizontalRatio)

            // This is the size we'd need to draw
            val newWidth = (width * minRatio).toInt()
            val newHeight = (height * minRatio).toInt()
            renderer.setVideoSize(newWidth, newHeight)
        }

        override fun onRenderedFirstFrame() {}

        ////////////////////////////////////////////////////////////////////////////////////////////
        // SharedPreferences.OnSharedPreferenceChangeListener

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences,
            key: String
        ) {
            when (key) {
                "scale_type" -> renderer.mode = mode
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        // Private helper methods

        private fun onPause() {
            glThread.onPause()
        }

        private fun onResume() {
            glThread.onResume()
        }

        init {
            Timber.d("Creating new GlEngine")
            PreferenceManager.getDefaultSharedPreferences(this@MovieLiveWallpaperService)
                    .registerOnSharedPreferenceChangeListener(this)
            val configChooser = BaseConfigChooser.SimpleEGLConfigChooser(true)
            val contextFactory = DefaultContextFactory()
            val windowSurfaceFactory = DefaultWindowSurfaceFactory()

            exoMediaPlayer.addVideoListener(this)
            glThread = GLThread(renderer, configChooser, contextFactory, windowSurfaceFactory, null)
            glThread.start()
            glThread.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
}