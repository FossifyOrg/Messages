package org.fossify.messages.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.messages.extensions.getConversations
import org.fossify.messages.extensions.getThreadParticipants
import org.fossify.messages.extensions.toPerson
import org.fossify.messages.models.Conversation


class ShortcutHelper(private val context: Context) {
    val contactsHelper = SimpleContactsHelper(context)

    fun getShortcuts(): List<ShortcutInfoCompat> {
        return ShortcutManagerCompat.getDynamicShortcuts(context)
    }

    fun getShortcut(threadId: Long): ShortcutInfoCompat? {
        return getShortcuts().find { it.id == threadId.toString() }
    }

    fun createOrUpdateShortcut(conv: Conversation): ShortcutInfoCompat {
        val participants = context.getThreadParticipants(conv.threadId, null)
        val persons: Array<Person> = participants.map { it.toPerson(context) }.toTypedArray()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms://messages/thread/${conv.threadId}")).apply {
            putExtra(THREAD_ID, conv.threadId)
            putExtra(THREAD_TITLE, conv.title)
            putExtra(IS_RECYCLE_BIN, false) // TODO: verify that thread isn't in recycle bin
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, conv.threadId.toString()).apply {
            setShortLabel(conv.title.substring(0, if (conv.title.length > 11) 11 else conv.title.length - 1))
            setLongLabel(conv.title)
            setIsConversation()
            setLongLived(true)
            setPersons(persons)
            setIntent(intent)
            if (!conv.isGroupConversation) {
                setIcon(persons[0].icon)
            } else {
                val icon = IconCompat.createWithBitmap(contactsHelper.getColoredGroupIcon(conv.title).toBitmap())
                setIcon(icon)
            }
            addCapabilityBinding("actions.intent.SEND_MESSAGE")
        }.build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        return shortcut
    }

    fun createOrUpdateShortcut(threadId: Long): ShortcutInfoCompat? {
        val convs = context.getConversations(threadId)
        if (convs.isEmpty()) {
            return null
        }

        val conv = convs[0]
        return createOrUpdateShortcut(conv)
    }

    /**
     * Report the usage of a thread. create shortcut if it doesn't exist
     */
    fun reportThreadUsage(threadId: Long) {
        val shortcut = getShortcut(threadId)
        if (shortcut == null) {
            createOrUpdateShortcut(threadId)
            return
        }
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    fun removeShortcutForThread(threadId: Long) {
        val shortcut = getShortcut(threadId) ?: return
        ShortcutManagerCompat.removeDynamicShortcuts(context, mutableListOf(shortcut.id))
    }

    fun removeAllShortcuts() {
        val scs = getShortcuts()
        if (scs.isEmpty())
            return
        ShortcutManagerCompat.removeLongLivedShortcuts(context, scs.map { it.id })
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }
}

fun ShortcutInfoCompat.toFormattedString(): String {
    return "$this \n\t" +
        "id : $id\n\t" +
        "$shortLabel : $longLabel\n\t" +
        "intent : $intent"
}
