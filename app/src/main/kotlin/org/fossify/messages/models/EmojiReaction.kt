package org.fossify.messages.models

data class EmojiReaction(
    val reactionMessageId: Long,
    val senderPhoneNumber: String,
    val emoji: String,
    val originalMessageText: String,
)
