package net.microink.cl.mmr.retriever.av

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer


/**
 * @author Cass
 * @Date 2024/12/31 16:14
 * @version v1.0
 */
class AVGLHelper {

    companion object {
        private const val TAG = "AVGLHelper"

        private const val EGL_RECORDABLE_ANDROID = 0x3142
        private const val EGL_OPENGL_ES2_BIT = 4
    }

    private var mSurfaceTexture: SurfaceTexture? = null
    private var mTextureRender: AVTextureRender? = null

    private var mEglDisplay: EGLDisplay? = EGL14.EGL_NO_DISPLAY
    private var mEglContext: EGLContext? = EGL14.EGL_NO_CONTEXT
    private var mEglSurface: EGLSurface? = EGL14.EGL_NO_SURFACE

    fun init(st: SurfaceTexture?) {
        mSurfaceTexture = st
        initGL()

        makeCurrent()
        mTextureRender = AVTextureRender()
    }

    private fun initGL() {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException(
                "eglGetdisplay failed : " +
                        GLUtils.getEGLErrorString(EGL14.eglGetError())
            )
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }

        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )

        val configs: Array<EGLConfig?> = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                mEglDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
        }

        // Configure context for OpenGL ES 2.0.
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        mEglContext = EGL14.eglCreateContext(
            mEglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        AVGLUtil.checkEglError("eglCreateContext")
        if (mEglContext == null) {
            throw RuntimeException("null context")
        }

        // Create a window surface, and attach it to the Surface we received.
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        mEglSurface = EGL14.eglCreateWindowSurface(
            mEglDisplay, configs[0], Surface(mSurfaceTexture),
            surfaceAttribs, 0
        )
        AVGLUtil.checkEglError("eglCreateWindowSurface")
        if (mEglSurface == null) {
            throw RuntimeException("surface was null")
        }
    }

    fun release() {
        if (null != mSurfaceTexture) mSurfaceTexture!!.release()
    }

    private fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        val textureID = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID)
        AVGLUtil.checkEglError("glBindTexture textureID")

        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        AVGLUtil.checkEglError("glTexParameter")

        return textureID
    }

    fun drawFrame(st: SurfaceTexture, textureID: Int) {
        st.updateTexImage()
        mTextureRender?.drawFrame(st, textureID)
    }

    fun readPixels(width: Int, height: Int): Bitmap {
        val pixelBuffer = ByteBuffer.allocateDirect(4 * width * height)
        pixelBuffer.position(0)
        GLES20.glReadPixels(
            0,
            0,
            width,
            height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            pixelBuffer
        )

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pixelBuffer.position(0)
        bmp.copyPixelsFromBuffer(pixelBuffer)

        return bmp
    }
}