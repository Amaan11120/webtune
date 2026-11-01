package com.amaan.webtune

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.util.concurrent.CountDownLatch

class App : Application() {
    companion object {
        val ready = CountDownLatch(1)
        @Volatile var err: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
            } catch (e: Exception) {
                err = e.message ?: "Init error"
            } finally {
                ready.countDown()
            }
            // update alag thread me, download ko block nahi karega
            try {
                val sp = getSharedPreferences("wt", MODE_PRIVATE)
                val now = System.currentTimeMillis()
                if (err == null && now - sp.getLong("upd", 0) > 24 * 3600 * 1000L) {
                    YoutubeDL.getInstance().updateYoutubeDL(this)
                    sp.edit().putLong("upd", now).apply()
                }
            } catch (e: Exception) { }
        }.start()
    }
}
