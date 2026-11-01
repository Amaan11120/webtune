package com.amaan.webtune

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object Bus { @Volatile var js: ((String) -> Unit)? = null }

class DownloadService : Service() {
    private val exec = Executors.newSingleThreadExecutor()
    private val pending = AtomicInteger(0)
    private val ch = "webtune"
    private var doneId = 100

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val url = i?.getStringExtra("url") ?: return START_NOT_STICKY
        val mode = i.getStringExtra("mode") ?: "video"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(ch, "Downloads", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, progress("Starting…", 0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        pending.incrementAndGet()
        exec.execute { work(url, mode) }
        return START_NOT_STICKY
    }

    private fun progress(text: String, p: Int): Notification =
        Notification.Builder(this, ch)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Web Tune")
            .setContentText(text)
            .setProgress(100, p, p == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun notify(id: Int, n: Notification) =
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, n)

    private fun finishNote(title: String, text: String) {
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        notify(doneId++, Notification.Builder(this, ch)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title).setContentText(text)
            .setContentIntent(open).setAutoCancel(true).build())
    }

    private fun js(c: String) { Bus.js?.invoke(c) }

    private fun work(url: String, mode: String) {
        try {
            App.ready.await(); App.err?.let { throw Exception(it) }
            val tmp = File(cacheDir, "dl" + System.currentTimeMillis())
            tmp.mkdirs()
            val req = YoutubeDLRequest(url.trim())
            req.addOption("-o", tmp.absolutePath + "/%(title).80s.%(ext)s")
            req.addOption("--no-playlist"); req.addOption("-N", "4"); req.addOption("--no-mtime")
            if (mode == "audio") {
                req.addOption("-x")
                req.addOption("--audio-format", "mp3")
                req.addOption("--audio-quality", "0")
            } else {
                req.addOption("-f", "bv*[height<=720]+ba/b[height<=720]/b")
                req.addOption("--merge-output-format", "mp4")
            }
            var last = -1
            YoutubeDL.getInstance().execute(req) { p, _, _ ->
                val v = p.toInt().coerceIn(0, 100)
                if (v != last) {
                    last = v
                    notify(1, progress("Downloading… $v%", v))
                    js("onProg($v)")
                }
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
            finishNote("Saved ✔", saved)
            js("onDone(${JSONObject.quote(saved)})")
        } catch (e: Exception) {
            val m = (e.message ?: "Error").take(200)
            finishNote("Download failed", m)
            js("onErr(${JSONObject.quote(m)})")
        } finally {
            if (pending.decrementAndGet() == 0) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
}
