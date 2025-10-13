package org.fossify.messages.extensions

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.onScroll(callback: (dx: Int, dy: Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            callback(dx, dy)
        }
    })
}
