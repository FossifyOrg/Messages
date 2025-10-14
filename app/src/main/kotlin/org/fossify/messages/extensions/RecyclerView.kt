package org.fossify.messages.extensions

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.onScroll(
    onScrolled: ((dx: Int, dy: Int) -> Unit),
    onScrollStateChanged: ((newState: Int) -> Unit)
) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            onScrolled.invoke(dx, dy)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            onScrollStateChanged.invoke(newState)
        }
    })
}
