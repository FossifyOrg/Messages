package org.fossify.messages.helpers

import android.util.LruCache
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.models.NamePhoto

private const val CACHE_SIZE = 512

object MessagingCache {
    val namePhoto = LruCache<String, NamePhoto>(CACHE_SIZE)
    internal val participantsCache = ThreadParticipantsCache(CACHE_SIZE)
}

internal class ThreadParticipantsCache(private val maxSize: Int) {
    private val lock = Any()
    private val entries = LruCache<Long, Entry>(maxSize)

    fun get(threadId: Long, recipientNumbers: List<String>): ArrayList<SimpleContact>? {
        val key = recipientNumbers.toRecipientCacheKey()
        synchronized(lock) {
            val entry = entries.get(threadId) ?: return null
            if (entry.recipientNumbers != key) {
                entries.remove(threadId)
                return null
            }

            return entry.participants.deepCopy()
        }
    }

    fun put(
        threadId: Long,
        recipientNumbers: List<String>,
        participants: ArrayList<SimpleContact>,
    ) {
        if (participants.isEmpty()) return
        synchronized(lock) {
            entries.put(
                threadId,
                Entry(
                    recipientNumbers = recipientNumbers.toRecipientCacheKey(),
                    participants = participants.deepCopy()
                )
            )
        }
    }

    fun remove(threadId: Long) {
        synchronized(lock) {
            entries.remove(threadId)
        }
    }

    fun evictAll() {
        synchronized(lock) {
            entries.evictAll()
        }
    }

    private data class Entry(
        val recipientNumbers: List<String>,
        val participants: ArrayList<SimpleContact>,
    )
}

private fun List<String>.toRecipientCacheKey(): List<String> {
    return map { it.trim() }
}

private fun ArrayList<SimpleContact>.deepCopy(): ArrayList<SimpleContact> {
    return map { contact ->
        contact.copy(
            phoneNumbers = ArrayList(contact.phoneNumbers),
            birthdays = ArrayList(contact.birthdays),
            anniversaries = ArrayList(contact.anniversaries)
        )
    }.toCollection(ArrayList())
}
