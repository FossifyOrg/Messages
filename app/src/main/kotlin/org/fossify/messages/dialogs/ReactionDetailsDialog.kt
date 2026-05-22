package org.fossify.messages.dialogs

import android.view.ViewGroup
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.views.MyTextView
import org.fossify.messages.R
import org.fossify.messages.databinding.DialogReactionDetailsBinding

class ReactionDetailsDialog(
    private val activity: BaseSimpleActivity,
    reactions: List<String>,
) {
    init {
        val binding = DialogReactionDetailsBinding.inflate(activity.layoutInflater).apply {
            val rowPadding = 8.dpToPx()
            reactions.forEach { reaction ->
                dialogReactionDetailsHolder.addView(
                    MyTextView(activity).apply {
                        layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        text = reaction
                        setTextColor(activity.getProperTextColor())
                        textSize = REACTION_TEXT_SIZE
                        setPadding(0, rowPadding, 0, rowPadding)
                    }
                )
            }
        }

        activity.getAlertDialogBuilder().apply {
            activity.setupDialogStuff(binding.root, this, R.string.reactions)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * activity.resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val REACTION_TEXT_SIZE = 22f
    }
}
