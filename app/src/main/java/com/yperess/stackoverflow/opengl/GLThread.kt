// Copyright (c) 2018 Magicleap. All right reserved.

package com.yperess.stackoverflow.opengl

import android.view.SurfaceHolder
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLSurfaceView
import android.util.Log
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 *
 */
internal class GLThread(
    private val renderer: GLSurfaceView.Renderer,
    private val eglConfigChooser: GLSurfaceView.EGLConfigChooser,
    private val eglContextFactory: GLSurfaceView.EGLContextFactory,
    private val eglWindowSurfaceFactory: GLSurfaceView.EGLWindowSurfaceFactory,
    private val glWrapper: GLSurfaceView.GLWrapper?
) : Thread() {

    private val glThreadManager = GLThreadManager()
    private var eglOwner: GLThread? = null

    lateinit var surfaceHolder: SurfaceHolder
        private set
    private var sizeChanged = true

    // Once the thread is started, all accesses to the following member
    // variables are protected by the glThreadManager monitor
    var done: Boolean = false
    private var paused: Boolean = false
    private var hasSurface: Boolean = false
    private var waitingForSurface: Boolean = false
    private var haveEgl: Boolean = false
    private var width: Int = 0
    private var height: Int = 0
    private var _renderMode: Int = 0
    private var requestRender: Boolean = false
    private var _eventsWaiting: Boolean = false
    private val eventQueue = ArrayList<Runnable>()
    private var eglHelper: EglHelper? = null

    private val isDone: Boolean
        get() = glThreadManager.withLock { done }

    var renderMode: Int
        get() = glThreadManager.withLock { _renderMode }
        set(renderMode) {
            if (renderMode != GLSurfaceView.RENDERMODE_WHEN_DIRTY &&
                    renderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                throw IllegalArgumentException("renderMode")
            }
            glThreadManager.withLock {
                _renderMode = renderMode
                if (renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                    glThreadManager.signalAll()
                }
            }
        }

    private val event: Runnable?
        get() {
            synchronized(this) {
                if (eventQueue.size > 0) {
                    return eventQueue.removeAt(0)
                }

            }
            return null
        }

    init {
        done = false
        width = 0
        height = 0
        requestRender = true
        _renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun run() {
        name = "GLThread $id"
        Timber.i("starting tid=%d", id)

        try {
            guardedRun()
        } catch (e: InterruptedException) {
            // fall thru and exit normally
        } finally {
            glThreadManager.threadExiting(this)
        }
    }

    /*
	 * This private method should only be called inside a synchronized(glThreadManager) block.
	 */
    private fun stopEglLocked() {
        if (haveEgl) {
            haveEgl = false
            eglHelper!!.destroySurface()
            glThreadManager.releaseEglSurface(this)
        }
    }

    @Throws(InterruptedException::class)
    private fun guardedRun() {
        Timber.i("guardedRun()")
        eglHelper = EglHelper(eglConfigChooser, eglContextFactory, eglWindowSurfaceFactory,
                glWrapper)
        try {
            var gl: GL10? = null
            var tellRendererSurfaceCreated = true
            var tellRendererSurfaceChanged = true

            /*
			 * This is our main activity thread's loop, we go until asked to quit.
			 */
            while (!isDone) {
                /*
				 * Update the asynchronous state (window size)
				 */
                var w = 0
                var h = 0
                var changed = false
                var needStart = false
                var eventsWaiting = false

                glThreadManager.withLock {
                    while (true) {
                        // Manage acquiring and releasing the SurfaceView
                        // surface and the EGL surface.
                        if (paused) {
                            stopEglLocked()
                        }
                        if (!hasSurface) {
                            if (!waitingForSurface) {
                                stopEglLocked()
                                waitingForSurface = true
                                glThreadManager.signalAll()
                            }
                        } else {
                            if (!haveEgl) {
                                if (glThreadManager.tryAcquireEglSurface(this)) {
                                    haveEgl = true
                                    eglHelper!!.start()
                                    requestRender = true
                                    needStart = true
                                }
                            }
                        }

                        // Check if we need to wait. If not, update any state
                        // that needs to be updated, copy any state that
                        // needs to be copied, and use "break" to exit the
                        // wait loop.

                        if (done) {
                            return
                        }

                        if (_eventsWaiting) {
                            eventsWaiting = true
                            _eventsWaiting = false
                            break
                        }

                        if (!paused && hasSurface && haveEgl && width > 0 && height > 0
                                && (requestRender || _renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY)) {
                            changed = sizeChanged
                            w = width
                            h = height
                            sizeChanged = false
                            requestRender = false
                            if (hasSurface && waitingForSurface) {
                                changed = true
                                waitingForSurface = false
                                glThreadManager.signalAll()
                            }
                            break
                        }

                        // By design, this is the only place where we wait().

                        Timber.i("waiting tid=%d", id)
                        glThreadManager.await()
                    }
                } // end of synchronized(glThreadManager)

                /*
				 * Handle queued events
				 */
                if (eventsWaiting) {
                    var r: Runnable? = null
                    while (event.also { r = it } != null) {
                        r!!.run()
                        if (isDone) {
                            return
                        }
                    }
                    // Go back and see if we need to wait to render.
                    continue
                }

                if (needStart) {
                    tellRendererSurfaceCreated = true
                    changed = true
                }
                if (changed) {
                    gl = eglHelper!!.createSurface(surfaceHolder) as GL10
                    tellRendererSurfaceChanged = true
                }
                if (tellRendererSurfaceCreated) {
                    renderer.onSurfaceCreated(gl, eglHelper!!.eglConfig)
                    tellRendererSurfaceCreated = false
                }
                if (tellRendererSurfaceChanged) {
                    renderer.onSurfaceChanged(gl, w, h)
                    tellRendererSurfaceChanged = false
                }
                if (w > 0 && h > 0) {
                    /* draw a frame here */
                    renderer.onDrawFrame(gl)

                    /*
					 * Once we're done with GL, we need to call swapBuffers() to instruct the system to display the
					 * rendered frame
					 */
                    eglHelper!!.swap()
                    Thread.sleep(10)
                }
            }
        } finally {
            /*
			 * clean-up everything...
			 */
            glThreadManager.withLock {
                stopEglLocked()
                eglHelper!!.finish()
            }
        }
    }

    fun requestRender() {
        glThreadManager.withLock {
            requestRender = true
            glThreadManager.signalAll()
        }
    }

    fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
        glThreadManager.withLock {
            Timber.i("surfaceCreated tid=%d", id)
            hasSurface = true
            glThreadManager.signalAll()
        }
    }

    fun surfaceDestroyed() {
        glThreadManager.withLock {
            Timber.i("surfaceDestroyed tid=%d", id)
            hasSurface = false
            glThreadManager.signalAll()
            while (!waitingForSurface && isAlive && !done) {
                try {
                    glThreadManager.await()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }

            }
        }
    }

    fun onPause() {
        glThreadManager.withLock {
            paused = true
            glThreadManager.signalAll()
        }
    }

    fun onResume() {
        glThreadManager.withLock {
            paused = false
            requestRender = true
            glThreadManager.signalAll()
        }
    }

    fun onWindowResize(w: Int, h: Int) {
        glThreadManager.withLock {
            width = w
            height = h
            sizeChanged = true
            glThreadManager.signalAll()
        }
    }

    fun requestExitAndWait() {
        // don't call this from GLThread thread or it is a guaranteed
        // deadlock!
        glThreadManager.withLock {
            done = true
            glThreadManager.signalAll()
        }
        try {
            join()
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        }

    }

    /**
     * Queue an "event" to be run on the GL rendering thread.
     *
     * @param r
     * the runnable to be run on the GL rendering thread.
     */
    fun queueEvent(r: Runnable) {
        synchronized(this) {
            eventQueue.add(r)
            glThreadManager.withLock {
                _eventsWaiting = true
                glThreadManager.signalAll()
            }
        }
    }

    private inner class GLThreadManager : ReentrantLock() {

        private val condition = newCondition()

        fun signalAll() {
            if (isHeldByCurrentThread) {
                condition.signalAll()
            }
        }

        fun await() {
            condition.await()
        }

        @Synchronized
        fun threadExiting(thread: GLThread) {
            Timber.i("exiting tid=%d", thread.id)
            thread.done = true
            if (eglOwner === thread) {
                eglOwner = null
            }
            signalAll()
        }

        /*
		 * Tries once to acquire the right to use an EGL surface. Does not block.
		 *
		 * @return true if the right to use an EGL surface was acquired.
		 */
        @Synchronized
        fun tryAcquireEglSurface(thread: GLThread): Boolean {
            if (eglOwner === thread || eglOwner == null) {
                eglOwner = thread
                signalAll()
                return true
            }
            return false
        }

        @Synchronized
        fun releaseEglSurface(thread: GLThread) {
            if (eglOwner === thread) {
                eglOwner = null
            }
            signalAll()
        }
    }
}