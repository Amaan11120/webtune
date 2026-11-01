package com.amaan.webtune

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

class RgbBorder(
    private val radius: Float,
    private val inset: Float,
    private val width: Float
) : Drawable() {
    private val colors = intArrayOf(
        0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
        0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt()
    )
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF120D24.toInt() }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val m = Matrix()
    private var shader: SweepGradient? = null
    private var angle = 0f
    private val anim = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { angle = it.animatedValue as Float; invalidateSelf() }
    }

    fun start() = anim.start()
    fun stop() = anim.cancel()

    override fun onBoundsChange(b: Rect) { shader = null }

    override fun draw(c: Canvas) {
        val b = bounds
        val r = RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom + radius * 2)
        if (shader == null) shader = SweepGradient(b.exactCenterX(), b.exactCenterY(), colors, null)
        m.setRotate(angle, b.exactCenterX(), b.exactCenterY())
        shader!!.setLocalMatrix(m)
        line.shader = shader
        c.drawRoundRect(r, radius, radius, fill)
        line.strokeWidth = width * 5; line.alpha = 35
        c.drawRoundRect(r, radius, radius, line)
        line.strokeWidth = width * 3; line.alpha = 70
        c.drawRoundRect(r, radius, radius, line)
        line.strokeWidth = width; line.alpha = 255
        c.drawRoundRect(r, radius, radius, line)
    }

    override fun setAlpha(a: Int) {}
    override fun setColorFilter(f: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
