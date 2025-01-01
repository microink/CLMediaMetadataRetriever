package net.microink.cl.mmr.retriever

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import net.microink.cl.mmr.retriever.av.AVFrameCapture

class CLMediaMetadataRetriever {

    private var context: Context? = null
    private var path: String? = null
    private var uri: Uri? = null

    private var frameCapture: AVFrameCapture? = null
    private var retriever: MediaMetadataRetriever? = null

    /**
     * init
     */
    fun init(context: Context, path: String, initListener: InitListener) {
        if (null != frameCapture) {
            initListener.initFailed(IllegalStateException("It's already initialized"))
            return
        }
        this.context = context
        this.path = path
        val avFrameCapture = AVFrameCapture()
        avFrameCapture.setDataSource(path)

        // set surface width and height
        retriever = MediaMetadataRetriever()
        retriever?.setDataSource(path)
//        val bitmap = retriever?.getFrameAtTime(100 * 1000,
//            MediaMetadataRetriever.OPTION_CLOSEST)
//        bitmap?.let {
//            val width = it.width
//            val height = it.height
//
//            avFrameCapture.setTargetSize(width, height)
//        }
        val width = getVideoWidth()
        val height = getVideoHeight()
        width?.let { w ->
            height?.let { h ->
                avFrameCapture.setTargetSize(w, h)
            }
        }

        // init avFrameCapture
        avFrameCapture.init()

        this.frameCapture = avFrameCapture
        initListener.initSuccess()
    }

    /**
     * init
     */
    fun init(context: Context, uri: Uri, initListener: InitListener) {
        if (null != frameCapture) {
            initListener.initFailed(IllegalStateException("It's already initialized"))
            return
        }
        this.context = context
        this.uri = uri
        val avFrameCapture = AVFrameCapture()
        avFrameCapture.setDataSource(uri)

        // set surface width and height
        retriever = MediaMetadataRetriever()
        retriever?.setDataSource(context, uri)
        val width = getVideoWidth()
        val height = getVideoHeight()
        width?.let { w ->
            height?.let { h ->
                avFrameCapture.setTargetSize(w, h)
            }
        }

        // init avFrameCapture
        avFrameCapture.init()

        this.frameCapture = avFrameCapture
        initListener.initSuccess()
    }

    /**
     * get frame at time um
     */
    fun getFrameAtTime(time: Long): Bitmap? {
        frameCapture?.let { capture ->
            context?.let { contextTemp ->
                return capture.getFrameAtTime(time, contextTemp)
            }
        }
        return null
    }

    /**
     * parse video's Metadata
     */
    fun extractMetadata(keyCode: Int): String? {
        retriever?.let {
            val result = it.extractMetadata(keyCode)
            return result
        }
        return null
    }

    /**
     * get video length ms
     */
    fun getVideoMsLength(): Int? {
        retriever?.let {
            val result = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            return result?.toInt()
        }
        return null
    }

    /**
     * get video width
     */
    fun getVideoWidth(): Int? {
        retriever?.let {
            val result = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            return result?.toInt()
        }
        return null
    }

    /**
     * get video height
     */
    fun getVideoHeight(): Int? {
        retriever?.let {
            val result = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            return result?.toInt()
        }
        return null
    }

    /**
     * get video rotation
     */
    fun getVideoRotation(): Int? {
        retriever?.let {
            val result = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            return result?.toInt()
        }
        return null
    }

    /**
     * release
     */
    fun release() {
        frameCapture?.release()
        frameCapture = null
        retriever?.release()
        retriever = null
        path = null
        uri = null
        context = null
    }

    interface InitListener {
        fun initSuccess()
        fun initFailed(e: Exception)
    }
}