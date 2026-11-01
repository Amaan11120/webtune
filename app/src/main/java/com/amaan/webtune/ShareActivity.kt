package com.amaan.webtune

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class ShareActivity : Activity() {
    private lateinit var url: String
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun grad(r: Int) = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(0xFF00F0FF.toInt(), 0xFFFF2BD6.toInt())
    ).apply { cornerRadius = dp(r).toFloat() }

    private fun chip(label: String, mode: String, w: Float): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.BLACK)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = grad(14)
            layoutParams = LinearLayout.LayoutParams(0, dp(50), w).apply {
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            setOnClickListener { go(mode) }
        }

    private fun row(vararg b: Button) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        b.forEach { addView(it) }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) { finish(); return }
        url = Regex("https?://\\S+").find(text)?.value ?: text.trim()
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = FrameLayout(this)
        root.setBackgroundColor(0x99000000.toInt())
        root.setOnClickListener { finish() }

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            setPadding(dp(18), dp(18), dp(18), dp(24))
            background = GradientDrawable().apply {
                setColor(0xFF120D24.toInt())
                val r = dp(28).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setStroke(dp(1), 0x55FF2BD6)
            }
        }
        root.addView(sheet, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        sheet.addView(TextView(this).apply {
            text = "🎧  Web Tune"
            textSize = 22f
            setTextColor(0xFF00F0FF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        sheet.addView(TextView(this).apply {
            text = url
            textSize = 12f
            setTextColor(0x99FFFFFF.toInt())
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(4), 0, dp(14))
        })
        sheet.addView(TextView(this).apply {
            text = "🎬 Video"
            textSize = 13f
            setTextColor(0xCCFFFFFF.toInt())
            setPadding(dp(4), 0, 0, dp(2))
        })
        sheet.addView(row(chip("360p", "video:360", 1f),
            chip("480p", "video:480", 1f), chip("720p", "video:720", 1f)))
        sheet.addView(row(chip("1080p", "video:1080", 1f),
            chip("Best", "video:9999", 1f)))
        sheet.addView(TextView(this).apply {
            text = "🎵 Audio"
            textSize = 13f
            setTextColor(0xCCFFFFFF.toInt())
            setPadding(dp(4), dp(10), 0, dp(2))
        })
        sheet.addView(row(chip("MP3  (Best quality)", "audio", 1f)))
    }

    private fun go(mode: String) {
        val i = Intent(this, DownloadService::class.java)
        i.putExtra("url", url)
        i.putExtra("mode", mode)
        startForegroundService(i)
        Toast.makeText(this, "⬇ Download started", Toast.LENGTH_SHORT).show()
        finish()
    }
}
