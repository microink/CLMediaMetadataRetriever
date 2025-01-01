package net.microink.cl.mmr.retriever.av

import android.opengl.EGL14
import android.util.Log


/**
 * @author Cass
 * @Date 2024/12/31 16:16
 * @version v1.0
 */
class AVGLUtil {

    companion object {

        private const val TAG = "AVGLUtil"

        fun checkEglError(msg: String) {
            var failed = false
            var error: Int
            while ((EGL14.eglGetError().also { error = it }) != EGL14.EGL_SUCCESS) {
                Log.e(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error))
                failed = true
            }
            if (failed) {
                throw RuntimeException("EGL error encountered (see log)")
            }
        }
    }
}