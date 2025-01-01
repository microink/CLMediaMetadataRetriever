package net.microink.cl.mmr.retriever.av

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface


/**
 * @author Cass
 * @Date 2024/12/31 16:26
 * @version v1.0
 */
class AVFrameCapture {

    companion object {
        private const val TAG = "AVFrameCapture"
    }

    private var mGLThread: HandlerThread = HandlerThread("AVFrameCapture")
    private var mGLHandler: Handler
    private var mGLHelper: AVGLHelper = AVGLHelper()

    private val mDefaultTextureID = 10001

    private var mWidth = 1920
    private var mHeight = 1080

    private var mPath: String? = null
    private var uri: Uri? = null

    init {

        mGLThread.start()
        mGLHandler = Handler(mGLThread.looper)
    }

    fun setDataSource(path: String) {
        mPath = path
    }

    fun setDataSource(uri: Uri) {
        this.uri = uri
    }

    fun setTargetSize(width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    fun init() {
        mGLHandler.post {
            val st = SurfaceTexture(mDefaultTextureID)
            st.setDefaultBufferSize(mWidth, mHeight)
            mGLHelper.init(st)
        }
    }

    fun release() {
        mGLHandler.post {
            mGLHelper.release()
            mGLThread.quit()
        }
    }

    private val mWaitBitmap = Any()
    private var mBitmap: Bitmap? = null

    fun getFrameAtTime(frameTime: Long, context: Context): Bitmap? {
//        if (null == mPath || mPath!!.isEmpty()) {
//            throw RuntimeException("Illegal State")
//        }
        if (null == uri) {
            throw RuntimeException("uri must be not null")
        }

        mGLHandler.post { getFrameAtTimeImpl(frameTime, context) }

        synchronized(mWaitBitmap) {
            try {
                (mWaitBitmap as Object).wait()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        return mBitmap
    }

    @SuppressLint("SdCardPath")
    fun getFrameAtTimeImpl(frameTime: Long, context: Context) {
        val textureID: Int = mGLHelper.createOESTexture()
        val st = SurfaceTexture(textureID)
        val surface: Surface = Surface(st)
        var vd: AVVideoDecoder? = null
        mPath?.let {
            vd = AVVideoDecoder(context, it, surface, uri)
        }
        uri?.let {
            vd = AVVideoDecoder(context, mPath, surface, it)
        }
        st.setOnFrameAvailableListener {
            Log.i(TAG, "onFrameAvailable")
            mGLHelper.drawFrame(st, textureID)
            mBitmap = mGLHelper.readPixels(mWidth, mHeight)
            synchronized(mWaitBitmap) {
                (mWaitBitmap as Object).notify()
            }

            vd?.release()
            st.detachFromGLContext()
            st.setOnFrameAvailableListener(null)
            st.release()
            surface.release()
        }

        vd?.let {
            if (!it.prepare(frameTime)) {
                mBitmap = null
                synchronized(mWaitBitmap) {
                    (mWaitBitmap as Object).notify()
                }
            }
        }
    }
}