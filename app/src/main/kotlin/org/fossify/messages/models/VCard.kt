package org.fossify.messages.models

import android.content.Context
import ezvcard.VCard
import ezvcard.property.*
import org.fossify.commons.extensions.normalizePhoneNumber
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.format
import org.fossify.messages.helpers.parseNameFromVCard

private val displayedPropertyClasses = arrayOf(
    Telephone::class.java, Email::class.java, Organization::class.java, Birthday::class.java, Anniversary::class.java, Note::class.java
)

data class VCardWrapper(val vCard: VCard, val fullName: String?, val properties: List<VCardPropertyWrapper>, var expanded: Boolean = false) {

    companion object {

        fun from(context: Context, vCard: VCard): VCardWrapper {
            val properties = vCard.properties
                .filter { displayedPropertyClasses.contains(it::class.java) }
                .map { VCardPropertyWrapper.from(context, it) }
                .distinctBy { it.value }
            val fullName = vCard.parseNameFromVCard()

            return VCardWrapper(vCard, fullName, properties)
        }
    }
}

data class VCardPropertyWrapper(val value: String, val type: String, val property: VCardProperty) {

    companion object {
        private const val CELL = "CELL"
        private const val HOME = "HOME"
        private const val WORK = "WORK"

        private fun VCardProperty.getPropertyTypeString(context: Context): String {
            return when (parameters.type) {
                CELL -> context.getString(org.fossify.commons.R.string.mobile)
                HOME -> context.getString(org.fossify.commons.R.string.home)
                WORK -> context.getString(org.fossify.commons.R.string.work)
                else -> ""
            }
        }

        fun from(context: Context, property: VCardProperty): VCardPropertyWrapper {
            return property.run {
                when (this) {
                    is Telephone -> VCardPropertyWrapper(text.normalizePhoneNumber(), getPropertyTypeString(context), property)
                    is Email -> VCardPropertyWrapper(value, getPropertyTypeString(context), property)
                    is Organization -> VCardPropertyWrapper(
                        value = values.joinToString(),
                        type = context.getString(org.fossify.commons.R.string.work),
                        property = property
                    )

                    is Birthday -> VCardPropertyWrapper(
                        value = date.format(context.config.dateFormat),
                        type = context.getString(org.fossify.commons.R.string.birthday),
                        property = property
                    )

                    is Anniversary -> VCardPropertyWrapper(
                        value = date.format(context.config.dateFormat),
                        type = context.getString(org.fossify.commons.R.string.anniversary),
                        property = property
                    )

                    is Note -> VCardPropertyWrapper(value, context.getString(org.fossify.commons.R.string.notes), property)
                    else -> VCardPropertyWrapper("", "", property)
                }
            }
        }
    }
}
