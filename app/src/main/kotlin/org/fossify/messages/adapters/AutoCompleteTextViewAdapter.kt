package org.fossify.messages.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import org.fossify.commons.databinding.ItemContactWithNumberBinding
import org.fossify.commons.extensions.darkenColor
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.activities.SimpleActivity

class AutoCompleteTextViewAdapter(val activity: SimpleActivity, val contacts: ArrayList<SimpleContact>) : ArrayAdapter<SimpleContact>(activity, 0, contacts) {
    var resultList = ArrayList<SimpleContact>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contact = resultList.getOrNull(position)
        var listItem = convertView
        if (listItem == null || listItem.tag != contact?.name?.isNotEmpty()) {
            listItem = ItemContactWithNumberBinding.inflate(LayoutInflater.from(activity), parent, false).root
        }

        listItem.tag = contact?.name?.isNotEmpty()
        ItemContactWithNumberBinding.bind(listItem).apply {
            // clickable and focusable properties seem to break Autocomplete clicking, so remove them
            itemContactFrame.apply {
                isClickable = false
                isFocusable = false
            }

            val backgroundColor = activity.getProperBackgroundColor()
            itemContactFrame.setBackgroundColor(backgroundColor.darkenColor())
            itemContactName.setTextColor(backgroundColor.getContrastColor())
            itemContactNumber.setTextColor(backgroundColor.getContrastColor())

            if (contact != null) {
                itemContactName.text = contact.name
                itemContactNumber.text = contact.phoneNumbers.first().normalizedNumber
                SimpleContactsHelper(context).loadContactImage(contact.photoUri, itemContactImage, contact.name)
            }
        }

        return listItem
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            if (constraint != null) {
                val results = mutableListOf<SimpleContact>()
                val searchString = constraint.toString().normalizeString()
                contacts.forEach {
                    if (it.doesContainPhoneNumber(searchString) || it.name.contains(searchString, true)) {
                        results.add(it)
                    }
                }

                results.sortWith(compareBy { !it.name.startsWith(searchString, true) })

                filterResults.values = results
                filterResults.count = results.size
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                resultList.clear()
                @Suppress("UNCHECKED_CAST")
                resultList.addAll(results.values as List<SimpleContact>)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        override fun convertResultToString(resultValue: Any?) = (resultValue as? SimpleContact)?.name
    }

    override fun getItem(index: Int) = resultList[index]

    override fun getCount() = resultList.size
}
