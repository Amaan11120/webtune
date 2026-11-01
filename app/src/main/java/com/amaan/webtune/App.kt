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
                val sp = getSharedPreferences("wt", MODE_PRIVATE)
                val now = System.currentTimeMillis()
                if (now - sp.getLong("upd", 0) > 12 * 3600 * 1000L) {
                    try {
                        YoutubeDL.getInstance().updateYoutubeDL(this)
                        sp.edit().putLong("upd", now).apply()
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) {
                err = e.message ?: "Init error"
            } finally {
                ready.countDown()
            }
        }.start()
    }
}
