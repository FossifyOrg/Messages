package org.fossify.messages.dialogs

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.getPopupMenuTheme
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.databinding.ItemManageBlockedKeywordBinding
import org.fossify.messages.extensions.config

class ManageAllowedKeywordsAdapter(
    activity: BaseSimpleActivity,
    var allowedKeywords: ArrayList<String>,
    val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_allowed_keywords

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_copy_keyword).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_copy_keyword -> copyKeywordToClipboard()
            R.id.cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = allowedKeywords.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = allowedKeywords.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = allowedKeywords.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManageBlockedKeywordBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val allowedKeyword = allowedKeywords[position]
        holder.bindView(allowedKeyword, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, allowedKeyword)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = allowedKeywords.size

    private fun getSelectedItems() = allowedKeywords.filter { selectedKeys.contains(it.hashCode()) }

    private fun setupView(view: View, allowedKeyword: String) {
        ItemManageBlockedKeywordBinding.bind(view).apply {
            root.setupViewBackground(activity)
            manageBlockedKeywordHolder.isSelected = selectedKeys.contains(allowedKeyword.hashCode())
            manageBlockedKeywordTitle.apply {
                text = allowedKeyword
                setTextColor(textColor)
            }

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, allowedKeyword)
            }
        }
    }

    private fun showPopupMenu(view: View, allowedKeyword: String) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val allowedKeywordId = allowedKeyword.hashCode()
                when (item.itemId) {
                    R.id.cab_copy_keyword -> {
                        executeItemMenuOperation(allowedKeywordId) {
                            copyKeywordToClipboard()
                        }
                    }

                    R.id.cab_delete -> {
                        executeItemMenuOperation(allowedKeywordId) {
                            deleteSelection()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(allowedKeywordId: Int, callback: () -> Unit) {
        selectedKeys.add(allowedKeywordId)
        callback()
        selectedKeys.remove(allowedKeywordId)
    }

    private fun copyKeywordToClipboard() {
        val selectedKeyword = getSelectedItems().firstOrNull() ?: return
        activity.copyToClipboard(selectedKeyword)
        finishActMode()
    }

    private fun deleteSelection() {
        val deleteAllowedKeywords = HashSet<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            deleteAllowedKeywords.add(it)
            activity.config.removeAllowedKeyword(it)
        }

        allowedKeywords.removeAll(deleteAllowedKeywords)
        removeSelectedItems(positions)
        if (allowedKeywords.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
