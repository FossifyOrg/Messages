package org.fossify.messages.extensions

import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible

fun View.showWithAnimation(duration: Long = 250L) {
    if (!isVisible) {
        ObjectAnimator.ofFloat(
            this, "alpha", 0f, 1f
        ).apply {
            this.duration = duration
            doOnStart { visibility = View.VISIBLE }
        }.start()
    }
}

