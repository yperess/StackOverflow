package com.yperess.stackoverflow.opengl

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGL10

/**
 *
 */
class DefaultContextFactory : GLSurfaceView.EGLContextFactory {

    companion object {
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        private val ATTRIB_LIST = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
    }

    override fun createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext =
            egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, ATTRIB_LIST)

    override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
        egl.eglDestroyContext(display, context)
    }
}