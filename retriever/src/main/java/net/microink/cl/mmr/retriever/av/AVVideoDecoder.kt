package net.microink.cl.mmr.retriever.av

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import android.view.Surface
import java.io.IOException


/**
 * @author Cass
 * @Date 2024/12/31 16:30
 * @version v1.0
 */
class AVVideoDecoder(
    private var context: Context?,
    private val path: String?,
    surface: Surface,
    private val uri: Uri?) {

    companion object {
        private const val TAG = "AVVideoDecoder"

        const val VIDEO_MIME_PREFIX: String = "video/"
    }

    private lateinit var mMediaExtractor: MediaExtractor
    private lateinit var mMediaCodec: MediaCodec

    private var mSurface: Surface = surface

    //    private var mPath: String
    private var mVideoTrackIndex = -1

    init {
//        this.mPath = path

        initCodec()
    }

    fun prepare(time: Long): Boolean {
        return decodeFrameAt(time)
    }

    fun startDecode() {
    }

    fun release() {
        mMediaCodec.stop()
        mMediaCodec.release()

        mMediaExtractor.release()
        context = null
    }

    private fun initCodec(): Boolean {
        Log.i(TAG, "initCodec")
        mMediaExtractor = MediaExtractor()
        try {
            path?.let {
                mMediaExtractor.setDataSource(path)
            }
            uri?.let { uriTemp ->
                context?.let { contextTemp ->
                    mMediaExtractor.setDataSource(contextTemp, uriTemp, null)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        val trackCount = mMediaExtractor.trackCount
        for (i in 0 until trackCount) {
            val mf = mMediaExtractor.getTrackFormat(i)
            val mime = mf.getString(MediaFormat.KEY_MIME)
            if (mime!!.startsWith(VIDEO_MIME_PREFIX)) {
                mVideoTrackIndex = i
                break
            }
        }
        if (mVideoTrackIndex < 0) return false

        mMediaExtractor.selectTrack(mVideoTrackIndex)
        val mf = mMediaExtractor.getTrackFormat(mVideoTrackIndex)
        val mime = mf.getString(MediaFormat.KEY_MIME)
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mime!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        mMediaCodec.configure(mf, mSurface, null, 0)
        mMediaCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        mMediaCodec.start()
        Log.i(TAG, "initCodec end")

        return true
    }

    private var mIsInputEOS = false

    private fun decodeFrameAt(timeUs: Long): Boolean {
        Log.i(TAG, "decodeFrameAt $timeUs")
        mMediaExtractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        mIsInputEOS = false
        val inputState = CodecState()
        val outState = CodecState()
        var reachTarget: Boolean
        while (true) {
            if (!inputState.eos) handleCodecInput(inputState)

            if (inputState.outIndex < 0) {
                handleCodecOutput(outState)
                reachTarget = processOutputState(outState, timeUs)
            } else {
                reachTarget = processOutputState(inputState, timeUs)
            }

            if (reachTarget || outState.eos) {
                Log.i(TAG, "decodeFrameAt $timeUs reach target or EOS")
                break
            }

            inputState.outIndex = -1
            outState.outIndex = -1
        }

        return reachTarget
    }

    private fun processOutputState(state: CodecState, timeUs: Long): Boolean {
        if (state.outIndex < 0) return false

        if (state.info.presentationTimeUs < timeUs) {
            Log.i(TAG, "processOutputState presentationTimeUs " + state.info.presentationTimeUs)
            mMediaCodec.releaseOutputBuffer(state.outIndex, false)
            return false
        }

        Log.i(TAG, "processOutputState presentationTimeUs " + state.info.presentationTimeUs)
        mMediaCodec.releaseOutputBuffer(state.outIndex, true)
        return true
    }

    private class CodecState {
        var outIndex: Int = MediaCodec.INFO_TRY_AGAIN_LATER
        var info: BufferInfo = BufferInfo()
        var eos: Boolean = false
    }

    private fun handleCodecInput(state: CodecState) {

        while (!mIsInputEOS) {
            val inputBufferIndex = mMediaCodec.dequeueInputBuffer(10000)
            if (inputBufferIndex < 0) {
                continue
            }

            val inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex) ?: continue

            var readSize = mMediaExtractor.readSampleData(inputBuffer, 0)
            val presentationTimeUs = mMediaExtractor.sampleTime
            val flags = mMediaExtractor.sampleFlags

            var eos = !mMediaExtractor.advance()
            eos = eos or (readSize <= 0)
            eos = eos or ((flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0)

            Log.i(TAG, "input presentationTimeUs $presentationTimeUs isEOS $eos")

            if (eos && readSize < 0) readSize = 0

            if (readSize > 0 || eos) mMediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                readSize,
                presentationTimeUs,
                flags or (if (eos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
            )

            if (eos) {
                state.eos = true
                mIsInputEOS = true
                break
            }

            state.outIndex = mMediaCodec.dequeueOutputBuffer(state.info, 10000)
            if (state.outIndex >= 0) break
        }
    }

    private fun handleCodecOutput(state: CodecState) {
        state.outIndex = mMediaCodec.dequeueOutputBuffer(state.info, 10000)
        if (state.outIndex < 0) {
            return
        }

        if ((state.info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            state.eos = true
            Log.i(TAG, "reach output EOS " + state.info.presentationTimeUs)
        }
    }
}