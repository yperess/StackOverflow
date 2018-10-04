package com.yperess.stackoverflow

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.text.TextUtils
import android.view.Surface
import android.view.View
import com.google.android.exoplayer2.SimpleExoPlayer
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * In this renderer, the exo media player will be drawing to a surface texture we create, that
 * texture will then be drawn to the actual surface provided by the wallpaper service. This
 * class relies on proper OpenGL setup in #MovieWallpaperService.GlEnging
 */
class MovieWallpaperRenderer(
    private val exoMediaPlayer: SimpleExoPlayer,
    mode: MovieLiveWallpaperService.Mode,
    private val context: Context
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    var mode: MovieLiveWallpaperService.Mode = mode
        set(value) {
            field = value
            updateVertices()
        }
    private var videoWidth = 100
    private var videoHeight = 100
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var isDirty = false
    private var isAllocated = false

    private val quadVertices: FloatBuffer = floatArrayOf(
            -1.0f, -1.0f, 0.0f, // bottom left
            -1.0f, +1.0f, 0.0f, // top left
            +1.0f, -1.0f, 0.0f, // bottom right
            +1.0f, +1.0f, 0.0f  // top right
    ).let { coords ->
        ByteBuffer.allocateDirect(coords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(coords)
    }
    private val quadTexCoords: FloatBuffer = floatArrayOf(
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f).let { coords ->
        ByteBuffer.allocateDirect(coords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(coords)
    }
    private val textures = IntArray(1)
    private var programHandle = 0
    private var quadPositionParam = 0
    private var quadTexCoordParam = 0

    fun setVideoSize(width: Int, height: Int) {
        Timber.d("setVideoSize(%d, %d) video=%dx%d", width, height, videoWidth, videoHeight)
        if (videoWidth != width || videoHeight != height) {
            videoWidth = width
            videoHeight = height
            createSurfaceTexture()
            updateVertices()
        }
    }

    fun setSurfaceSize(width: Int, height: Int) {
        if (surfaceWidth != width || surfaceHeight != height) {
            surfaceWidth = width
            surfaceHeight = height
            updateVertices()
        }
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        quadVertices.position(0)
        quadTexCoords.position(0)

        // Allocate the texture)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glGenTextures(textures.size, textures, 0)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER,
                GL10.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER,
                GL10.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_CLAMP_TO_EDGE)

        // Create the program
        val vertexShader = readRawResource(context, R.raw.screenquad_vertex)?.let { shader ->
            compileShader(GLES20.GL_VERTEX_SHADER, shader)
        } ?: throw RuntimeException("Failed to load vertex shader")
        val fragmentShader = readRawResource(context, R.raw.screenquad_fragment)?.let { shader ->
            compileShader(GLES20.GL_FRAGMENT_SHADER, shader)
        } ?: throw RuntimeException("Failed to load fragment shader")

        programHandle = createAndLinkProgram(vertexShader, fragmentShader)
        GLES20.glUseProgram(programHandle)
        quadPositionParam = GLES20.glGetAttribLocation(programHandle, "a_Position")
        quadTexCoordParam = GLES20.glGetAttribLocation(programHandle, "a_TexCoord")

        isAllocated = true
        createSurfaceTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // We haven't gotten a size yet, dont' bother drawing anything
        val surfaceTexture = surfaceTexture ?: return

        if (isDirty) {
            surfaceTexture.updateTexImage()
            isDirty = false
        }

        // No need to test or write depth, the screen quad has arbitrary depth, and is the only
        // thing we're drawing
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])

        GLES20.glUseProgram(programHandle)

        // Set the vertex positions
        GLES20.glVertexAttribPointer(quadPositionParam, 3, GLES20.GL_FLOAT, false, 0, quadVertices)

        // Set the texture coordinates
        GLES20.glVertexAttribPointer(quadTexCoordParam, 2, GLES20.GL_FLOAT, false, 0, quadTexCoords)

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(quadPositionParam)
        GLES20.glEnableVertexAttribArray(quadTexCoordParam)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(quadPositionParam)
        GLES20.glDisableVertexAttribArray(quadTexCoordParam)

        // Restore the depth state for further drawing (probably not actually needed, just good
        // practice)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // SurfaceTexture.OnFrameAvailableListener

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        isDirty = true
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private helper methods

    private fun createSurfaceTexture() {
        Timber.d("createSurfaceTexture() isAllocated=%s, size=%dx%d",
                isAllocated, videoWidth, videoHeight)
        if (!isAllocated || videoWidth == 0 || videoHeight == 0) {
            return
        }
        surfaceTexture?.release()
        surfaceTexture = SurfaceTexture(textures[0]).apply {
            setDefaultBufferSize(videoWidth, videoHeight)
            setOnFrameAvailableListener(this@MovieWallpaperRenderer)
            exoMediaPlayer.setVideoSurface(Surface(this))
        }
    }

    private fun updateVertices() {
        Timber.d("updateVertices() video=%dx%d, surface=%dx%d, mode=%s",
                videoWidth, videoHeight, surfaceWidth, surfaceHeight, mode)
        if (surfaceWidth == 0 || surfaceHeight == 0) {
            return
        }
        val isHorizontal = videoWidth < surfaceWidth
        val values = when (mode) {
            MovieLiveWallpaperService.Mode.FIT_START -> {
                if (isHorizontal) {
                    val width = videoWidth * 2f / surfaceWidth
                    if (isLtr) {
                        // Align left
                        val right = -1 + width
                        floatArrayOf(
                                -1f, -1f, 0f,
                                -1f, +1f, 0f,
                                right, -1f, 0f,
                                right, +1f, 0f)
                    } else {
                        // Align right
                        val left = 1 - width
                        floatArrayOf(
                                left, -1f, 0f,
                                left, +1f, 0f,
                                1f, -1f, 0f,
                                1f, +1f, 0f)
                    }
                } else {
                    // Align top
                    val bottom = 1 - (videoHeight * 2f / surfaceHeight)
                    floatArrayOf(
                            -1f, bottom, 0f,
                            -1f, 1f, 0f,
                            +1f, bottom, 0f,
                            +1f, 1f, 0f)
                }
            }
            MovieLiveWallpaperService.Mode.FIT_CENTER -> {
                if (isHorizontal) {
                    // center horizontally
                    val width = videoWidth / surfaceWidth.toFloat()
                    floatArrayOf(
                            -width, -1f, 0f,
                            -width, +1f, 0f,
                            +width, -1f, 0f,
                            +width, +1f, 0f)
                } else {
                    // center vertically
                    val height = videoHeight / surfaceHeight.toFloat()
                    floatArrayOf(
                            -1f, -height, 0f,
                            -1f, +height, 0f,
                            +1f, -height, 0f,
                            +1f, +height, 0f)
                }
            }
            MovieLiveWallpaperService.Mode.FIT_END -> {
                if (isHorizontal) {
                    val width = videoWidth * 2f / surfaceWidth
                    if (!isLtr) {
                        // Align left
                        val right = -1 + width
                        floatArrayOf(
                                -1f, -1f, 0f,
                                -1f, +1f, 0f,
                                right, -1f, 0f,
                                right, +1f, 0f)
                    } else {
                        // Align right
                        val left = 1 - width
                        floatArrayOf(
                                left, -1f, 0f,
                                left, +1f, 0f,
                                1f, -1f, 0f,
                                1f, +1f, 0f)
                    }
                } else {
                    // Align bottom
                    val top = -1f + (videoHeight * 2f / surfaceHeight)
                    floatArrayOf(
                            -1f, -1f, 0f,
                            -1f, top, 0f,
                            +1f, -1f, 0f,
                            +1f, top, 0f)
                }
            }
            MovieLiveWallpaperService.Mode.FIT_XY -> floatArrayOf(
                    -1f, -1f, 0f,
                    -1f, +1f, 0f,
                    +1f, -1f, 0f,
                    +1f, +1f, 0f)
            MovieLiveWallpaperService.Mode.CENTER_CROP -> {
                val maxScale = Math.max(surfaceWidth / videoWidth, surfaceHeight / videoHeight)
                        .toFloat()
                val width = videoWidth * maxScale / surfaceWidth
                val height = videoHeight * maxScale / surfaceHeight
                floatArrayOf(
                        -width, -height, 0f,
                        -width, +height, 0f,
                        +width, -height, 0f,
                        +width, +height, 0f)
            }
        }
        Timber.d("newValues=[%s]", values.joinToString())
        quadVertices.position(0)
        quadVertices.put(values)
        quadVertices.position(0)
    }

    private val isLtr: Boolean
        get() = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_LTR
}