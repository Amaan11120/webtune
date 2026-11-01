package com.amaan.webtune

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.webkit.WebViewClient
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var loaded = false
    private var pending: String? = null
    private var initError: String? = null
    private val initThread = Thread {
        try {
            YoutubeDL.getInstance().init(application)
            FFmpeg.getInstance().init(application)
        } catch (e: Exception) {
            initError = e.message
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initThread.start()
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                loaded = true
                pending?.let { js("setUrl(${JSONObject.quote(it)})"); pending = null }
            }
        }
        setContentView(web)
        handle(intent)
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND) {
            i.getStringExtra(Intent.EXTRA_TEXT)?.let {
                if (loaded) js("setUrl(${JSONObject.quote(it)})") else pending = it
            }
        }
    }

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript(code, null) }

    inner class Bridge {
        @JavascriptInterface
        fun start(url: String, mode: String) {
            Thread { download(url.trim(), mode) }.start()
        }
    }

    private fun download(url: String, mode: String) {
        try {
            initThread.join()
            initError?.let { throw Exception(it) }
            val tmp = File(cacheDir, "dl" + System.currentTimeMillis())
            tmp.mkdirs()
            val req = YoutubeDLRequest(url)
            req.addOption("-o", tmp.absolutePath + "/%(title).80s.%(ext)s")
            req.addOption("--no-playlist")
            if (mode == "audio") {
                req.addOption("-x")
                req.addOption("--audio-format", "mp3")
                req.addOption("--audio-quality", "0")
            } else {
                req.addOption("-f", "bv*[height<=720]+ba/b[height<=720]/b")
                req.addOption("--merge-output-format", "mp4")
            }
            YoutubeDL.getInstance().execute(req) { p, _, _ ->
                js("onProg(${p.toInt().coerceIn(0, 100)})")
            }
            var saved = ""
            tmp.listFiles()?.forEach { f ->
                val mime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(f.extension.lowercase()) ?: "application/octet-stream"
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, f.name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/WebTune")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)!!
                contentResolver.openOutputStream(uri)!!.use { o ->
                    f.inputStream().use { it.copyTo(o) }
                }
                saved = f.name
            }
            tmp.deleteRecursively()
            js("onDone(${JSONObject.quote(saved)})")
        } catch (e: Exception) {
            js("onErr(${JSONObject.quote((e.message ?: "Error").take(200))})")
        }
    }
}
