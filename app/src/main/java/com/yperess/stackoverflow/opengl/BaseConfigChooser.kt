// Copyright (c) 2018 Magicleap. All right reserved.

package com.yperess.stackoverflow.opengl

import javax.microedition.khronos.egl.EGL10
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay


/**
 *
 */
internal abstract class BaseConfigChooser(
    protected var mConfigSpec: IntArray
) : GLSurfaceView.EGLConfigChooser {

    companion object {
        private const val EGL_OPENGL_ES2_BIT = 4
    }

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {
        val num_config = IntArray(1)
        egl.eglChooseConfig(display, mConfigSpec, null, 0, num_config)

        val numConfigs = num_config[0]

        if (numConfigs <= 0) {
            throw IllegalArgumentException("No configs match configSpec")
        }

        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs, num_config)
        return chooseConfig(egl, display, configs.requireNoNulls())
                ?: throw IllegalArgumentException("No config chosen")
    }

    internal abstract fun chooseConfig(egl: EGL10, display: EGLDisplay,
            configs: Array<EGLConfig>): EGLConfig?

    open class ComponentSizeChooser(// Subclasses can adjust these values:
        protected var redSize: Int,
        protected var greenSize: Int,
        protected var blueSize: Int,
        protected var alphaSize: Int,
        protected var depthSize: Int,
        protected var stencilSize: Int
    ) : BaseConfigChooser(intArrayOf(
            EGL10.EGL_RED_SIZE, redSize,
            EGL10.EGL_GREEN_SIZE, greenSize,
            EGL10.EGL_BLUE_SIZE, blueSize,
            EGL10.EGL_ALPHA_SIZE, alphaSize,
            EGL10.EGL_DEPTH_SIZE, depthSize,
            EGL10.EGL_STENCIL_SIZE, stencilSize,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_NONE)) {

        private val mValue: IntArray

        init {
            mValue = IntArray(1)
        }

        override fun chooseConfig(
            egl: EGL10,
            display: EGLDisplay,
            configs: Array<EGLConfig>
        ): EGLConfig? {
            var closestConfig: EGLConfig? = null
            var closestDistance = 1000
            for (config in configs) {
                val d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0)
                val s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0)
                if (d >= depthSize && s >= stencilSize) {
                    val r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0)
                    val g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0)
                    val b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0)
                    val a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0)
                    val distance = (Math.abs(r - redSize) + Math.abs(g - greenSize) + Math.abs(
                            b - blueSize)
                            + Math.abs(a - alphaSize))
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestConfig = config
                    }
                }
            }
            return closestConfig
        }

        private fun findConfigAttrib(egl: EGL10, display: EGLDisplay, config: EGLConfig,
                attribute: Int, defaultValue: Int): Int {

            return if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                mValue[0]
            } else defaultValue
        }
    }

    /**
     * This class will choose a supported surface as close to RGB565 as possible, with or without a depth buffer.
     *
     */
    class SimpleEGLConfigChooser(
        withDepthBuffer: Boolean
    ) : ComponentSizeChooser(4, 4, 4, 0,
            if (withDepthBuffer) 16 else 0, 0) {
        init {
            // Adjust target values. This way we'll accept a 4444 or
            // 555 buffer if there's no 565 buffer available.
            redSize = 5
            greenSize = 6
            blueSize = 5
        }
    }
}