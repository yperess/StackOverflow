package com.yperess.stackoverflow.opengl

import javax.microedition.khronos.egl.EGL10
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
/**
 *
 */
class DefaultWindowSurfaceFactory : GLSurfaceView.EGLWindowSurfaceFactory {

    override fun createWindowSurface(
        egl: EGL10,
        display: EGLDisplay,
        config: EGLConfig,
        nativeWindow: Any
    ): EGLSurface {
        var eglSurface: EGLSurface? = null
        while (eglSurface == null) {
            try {
                eglSurface = egl.eglCreateWindowSurface(display, config, nativeWindow, null)
            } catch (tr: Throwable) {
            } finally {
                if (eglSurface == null) {
                    try {
                        Thread.sleep(10)
                    } catch (ex: InterruptedException) {}
                }
            }
        }
        return eglSurface!!
    }

    override fun destroySurface(
        egl: EGL10,
        display: EGLDisplay,
        surface: EGLSurface
    ) {
        egl.eglDestroySurface(display, surface)
    }
}