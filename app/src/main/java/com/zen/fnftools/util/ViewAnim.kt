package com.zen.fnftools.util

import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Adds a tactile scale-down/scale-up "bounce" on press, since disabling the
 * default Material elevation animator leaves flat buttons feeling dead.
 */
fun View.setBouncy() {
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120)
                    .setInterpolator(DecelerateInterpolator()).start()
                if (event.action == MotionEvent.ACTION_UP) {
                    v.performClick()
                }
            }
        }
        true
    }
}

/**
 * Staggered fade + rise entrance for a set of views, called once on activity
 * launch so screens don't just snap into place.
 */
fun staggerIn(views: List<View>, startDelay: Long = 60L, step: Long = 70L) {
    views.forEachIndexed { i, v ->
        v.alpha = 0f
        v.translationY = 24f
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(startDelay + step * i)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}

fun View.pulseOnce() {
    animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).withEndAction {
        animate().scaleX(1f).scaleY(1f).setDuration(120).start()
    }.start()
}
