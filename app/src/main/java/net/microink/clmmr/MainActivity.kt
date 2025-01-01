package net.microink.clmmr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.microink.cl.mmr.retriever.CLMediaMetadataRetriever

class MainActivity : AppCompatActivity() {

    companion object {
        // request file code
        private const val REQUEST_CODE_CHOOSE_FILE = 1011
    }

    private val retriever: CLMediaMetadataRetriever = CLMediaMetadataRetriever()

    private lateinit var mIV: ImageView
    private lateinit var mBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mIV = findViewById(R.id.iv_main)
        mBtn = findViewById(R.id.btn_main)

        mBtn.setOnClickListener {
            openFile()
        }
    }

    private fun handleFileUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            retriever.init(this@MainActivity, uri,
                object : CLMediaMetadataRetriever.InitListener{
                override fun initSuccess() {
                    val seeTimeMs = 1000L
                    // ms to um
                    val bitmap = retriever.getFrameAtTime(seeTimeMs * 1000)
                    bitmap?.let {
                        launch(Dispatchers.Main) {
                            mIV.setImageBitmap(it)
                        }
                    }
                }

                override fun initFailed(e: Exception) {

                }

            })
        }
    }

    @Deprecated("")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CHOOSE_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                handleFileUri(uri)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        retriever.release()
    }

    /**
     * 打开文件选择器
     */
    @Suppress("DEPRECATION")
    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_FILE)
    }
}