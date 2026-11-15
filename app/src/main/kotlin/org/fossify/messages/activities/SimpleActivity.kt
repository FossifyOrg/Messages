package org.fossify.messages.activities

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.messages.R

open class SimpleActivity : BaseSimpleActivity() {
    private var navigationBarScrim: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNavigationBarScrim()
    }

    override fun onResume() {
        super.onResume()
        navigationBarScrim?.setBackgroundColor(getProperBackgroundColor())
    }

    /**
     * Edge-to-edge leaves the navigation bar fully transparent, so with 3-button navigation
     * the buttons overlap scrolled content and become hard to see. Draw an opaque background
     * behind the 3-button navigation bar. Gesture navigation has no tappable bar insets, so
     * it stays fully edge-to-edge.
     */
    private fun setupNavigationBarScrim() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // our own opaque scrim replaces the translucent system one
            window.isNavigationBarContrastEnforced = false
        }

        val decorView = window.decorView as? ViewGroup ?: return
        val scrim = View(this).apply {
            setBackgroundColor(getProperBackgroundColor())
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            visibility = View.GONE
        }
        decorView.addView(scrim, FrameLayout.LayoutParams(0, 0))
        navigationBarScrim = scrim

        ViewCompat.setOnApplyWindowInsetsListener(scrim) { view, insets ->
            updateNavigationBarScrim(view, insets)
            insets
        }
        ViewCompat.requestApplyInsets(scrim)
    }

    private fun updateNavigationBarScrim(scrim: View, insets: WindowInsetsCompat) {
        val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val tappable = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
        val params = scrim.layoutParams as FrameLayout.LayoutParams
        when {
            tappable.bottom > 0 && navBar.bottom > 0 -> {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = navBar.bottom
                params.gravity = Gravity.BOTTOM
            }

            tappable.left > 0 && navBar.left > 0 -> {
                params.width = navBar.left
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.START
            }

            tappable.right > 0 && navBar.right > 0 -> {
                params.width = navBar.right
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.END
            }

            else -> {
                scrim.visibility = View.GONE
                return
            }
        }
        scrim.layoutParams = params
        scrim.visibility = View.VISIBLE
    }

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher_red,
        R.mipmap.ic_launcher_pink,
        R.mipmap.ic_launcher_purple,
        R.mipmap.ic_launcher_deep_purple,
        R.mipmap.ic_launcher_indigo,
        R.mipmap.ic_launcher_blue,
        R.mipmap.ic_launcher_light_blue,
        R.mipmap.ic_launcher_cyan,
        R.mipmap.ic_launcher_teal,
        R.mipmap.ic_launcher,
        R.mipmap.ic_launcher_light_green,
        R.mipmap.ic_launcher_lime,
        R.mipmap.ic_launcher_yellow,
        R.mipmap.ic_launcher_amber,
        R.mipmap.ic_launcher_orange,
        R.mipmap.ic_launcher_deep_orange,
        R.mipmap.ic_launcher_brown,
        R.mipmap.ic_launcher_blue_grey,
        R.mipmap.ic_launcher_grey_black
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    override fun getRepositoryName() = "Messages"
}
