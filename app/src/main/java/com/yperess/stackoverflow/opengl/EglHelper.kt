package com.yperess.stackoverflow.opengl

import android.opengl.GLSurfaceView
import android.view.SurfaceHolder
import java.lang.IllegalStateException
import java.lang.RuntimeException
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL

/**
 *
 */
class EglHelper(
    private val eglConfigChooser: GLSurfaceView.EGLConfigChooser,
    private val eglContextFactory: GLSurfaceView.EGLContextFactory,
    private val eglWindowSurfaceFactory: GLSurfaceView.EGLWindowSurfaceFactory,
    private val glWrapper: GLSurfaceView.GLWrapper?
) {

    private var egl: EGL10? = null
    private var eglDisplay: EGLDisplay? = null
    var eglConfig: EGLConfig? = null
        private set
    private var eglSurface: EGLSurface? = null
    private var eglContext: EGLContext? = null

    fun start() {
        val egl = egl ?: (EGLContext.getEGL() as EGL10).also {
            egl = it
        }
        val display = eglDisplay ?: egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY).also {
            eglDisplay = it
        }
        val config = eglConfig ?: run {
            val version = IntArray(2)
            egl.eglInitialize(display, version)
            eglConfigChooser.chooseConfig(egl, display)
        }.also {
            eglConfig = it
        }
        if (eglContext == null) {
            eglContext = eglContextFactory.createContext(egl, display, config)
            if (eglContext == null || eglContext == EGL10.EGL_NO_CONTEXT) {
                throw RuntimeException("createContext failed")
            }
        }
        eglSurface = null
    }

    fun createSurface(holder: SurfaceHolder): GL {
        val egl = this.egl ?: throw IllegalStateException("egl must not be null")

        // The window size has changed, need to create a new surface
        if (eglSurface != null && eglSurface != EGL10.EGL_NO_SURFACE) {
            // unbind and destroy the old EGL surface
            egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT)
            eglWindowSurfaceFactory.destroySurface(egl, eglDisplay, eglSurface)
        }

        // Create an EGL surface for rendering
        eglSurface = eglWindowSurfaceFactory.createWindowSurface(egl, eglDisplay, eglConfig, holder)

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            throw RuntimeException("createWindowSurface failed")
        }

        // Before we can issue GL commands, we need to make sure the context is current and bound to
        // the surface
        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }

        return eglContext!!.gl.let { glWrapper?.wrap(it) ?: it }
    }

    fun swap(): Boolean {
        egl!!.eglSwapBuffers(eglDisplay, eglSurface)
        return egl!!.eglGetError() != EGL11.EGL_CONTEXT_LOST
    }

    fun destroySurface() {
        if (eglSurface != null && eglSurface != EGL10.EGL_NO_SURFACE) {
            egl!!.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT)
            eglWindowSurfaceFactory.destroySurface(egl, eglDisplay, eglSurface)
            eglSurface = null
        }
    }

    fun finish() {
        eglContext?.let {
            eglContextFactory.destroyContext(egl, eglDisplay, it)
            eglContext = null
        }
        eglDisplay?.let {
            egl?.eglTerminate(it)
            eglDisplay = null
        }
    }
}