@file:Suppress("MaxLineLength")

package org.fossify.messages.helpers

import org.fossify.messages.models.EmojiReaction
import org.fossify.messages.models.Message

data class ParsedEmojiReaction(
    val emoji: String,
    val originalMessage: String,
    val isRemoval: Boolean = false,
)

object EmojiReactionHelper {
    private val reactionPatterns: LinkedHashMap<Regex, (MatchResult) -> ParsedEmojiReaction?> = linkedMapOf(
        Regex(
            "(?s)^\u200a[^\u200b\u200a]*\u200b([^\u200b]*)\u200b[^\u200b\u200a]*\u200a(.*)\u200a[^\u200b\u200a]*\u200a\\Z"
        ) to { match ->
            ParsedEmojiReaction(match.groupValues[1], match.groupValues[2])
        }
    )

    private val removalPatterns: LinkedHashMap<Regex, (MatchResult) -> ParsedEmojiReaction?> = linkedMapOf(
        Regex(
            "(?s)^\u200a[^\u200c\u200a]*\u200c([^\u200c]*)\u200c[^\u200c\u200a]*\u200a(.*)\u200a[^\u200c\u200a]*\u200a\\Z"
        ) to { match ->
            ParsedEmojiReaction(match.groupValues[1], match.groupValues[2], isRemoval = true)
        }
    )

    init {
        addAppleTapbackPattern("❤️", "Loved", "Removed a heart from")
        addAppleTapbackPattern("👍", "Liked", "Removed a like from")
        addAppleTapbackPattern("👎", "Disliked", "Removed a dislike from")
        addAppleTapbackPattern("😂", "Laughed at", "Removed a laugh from")
        addAppleTapbackPattern("‼️", "Emphasized", "Removed an exclamation from")
        addAppleTapbackPattern("❓", "Questioned", "Removed a question mark from")

        reactionPatterns[Regex("""(?s)^Reacted (.+?) to ["“](.+?)["”]$""")] = { match ->
            if (match.groupValues.getOrNull(1) == "with a sticker") {
                null
            } else {
                ParsedEmojiReaction(match.groupValues[1], match.groupValues[2])
            }
        }
        removalPatterns[Regex("""(?s)^Removed (.+?) from ["“](.+?)["”]$""")] = { match ->
            ParsedEmojiReaction(match.groupValues[1], match.groupValues[2], isRemoval = true)
        }
    }

    fun parseEmojiReaction(body: String): ParsedEmojiReaction? {
        parseRemoval(body)?.let { return it }

        for ((pattern, parser) in reactionPatterns) {
            val match = pattern.find(body) ?: continue
            return parser(match) ?: continue
        }

        return null
    }

    fun applyEmojiReactions(messages: List<Message>): ArrayList<Message> {
        messages.forEach { message ->
            message.isEmojiReaction = false
            message.emojiReactions = emptyList()
        }

        val orderedMessages = messages.sortedWith(compareBy<Message> { it.date }.thenBy { it.id })
        orderedMessages.forEach { reactionMessage ->
            val parsedReaction = parseEmojiReaction(reactionMessage.body) ?: return@forEach
            val targetMessage = findTargetMessage(
                messages = orderedMessages,
                reactionMessage = reactionMessage,
                originalMessageText = parsedReaction.originalMessage,
            ) ?: return@forEach

            if (parsedReaction.isRemoval) {
                removeEmojiReaction(reactionMessage, parsedReaction, targetMessage)
            } else {
                saveEmojiReaction(reactionMessage, parsedReaction, targetMessage)
            }
        }

        return orderedMessages
            .filterNot { it.isEmojiReaction }
            .toCollection(ArrayList())
    }

    private fun addAppleTapbackPattern(emoji: String, addedPrefix: String, removedPrefix: String) {
        reactionPatterns[Regex("""(?s)^$addedPrefix ["“](.+?)["”]$""")] = { match ->
            ParsedEmojiReaction(emoji, match.groupValues[1])
        }
        removalPatterns[Regex("""(?s)^$removedPrefix ["“](.+?)["”]$""")] = { match ->
            ParsedEmojiReaction(emoji, match.groupValues[1], isRemoval = true)
        }
    }

    private fun parseRemoval(body: String): ParsedEmojiReaction? {
        for ((pattern, parser) in removalPatterns) {
            val match = pattern.find(body) ?: continue
            return parser(match) ?: continue
        }

        return null
    }

    private fun findTargetMessage(
        messages: List<Message>,
        reactionMessage: Message,
        originalMessageText: String,
    ): Message? {
        val originalMessageRegex = parseTruncatedMessage(originalMessageText)
        return messages
            .asReversed()
            .firstOrNull { candidate ->
                candidate.threadId == reactionMessage.threadId &&
                    candidate.id != reactionMessage.id &&
                    candidate.date <= reactionMessage.date &&
                    !candidate.isEmojiReaction &&
                    originalMessageRegex.matches(candidate.body.trim())
            }
    }

    private fun parseTruncatedMessage(originalMessageText: String): Regex {
        val reactionText = originalMessageText.trim()
        val delimiter = "\u2026"
        val index = reactionText.lastIndexOf(delimiter)
        val regexPattern = if (index == -1) {
            Regex.escape(reactionText)
        } else {
            val before = reactionText.take(index)
            Regex.escape(before) + ".*"
        }
        return Regex("^$regexPattern$", RegexOption.DOT_MATCHES_ALL)
    }

    private fun saveEmojiReaction(
        reactionMessage: Message,
        parsedReaction: ParsedEmojiReaction,
        targetMessage: Message,
    ) {
        val reaction = EmojiReaction(
            reactionMessageId = reactionMessage.id,
            senderPhoneNumber = reactionMessage.senderPhoneNumber,
            emoji = parsedReaction.emoji,
            originalMessageText = parsedReaction.originalMessage,
        )
        targetMessage.emojiReactions = targetMessage.emojiReactions
            .filterNot { it.senderPhoneNumber == reaction.senderPhoneNumber } + reaction
        reactionMessage.isEmojiReaction = true
    }

    private fun removeEmojiReaction(
        reactionMessage: Message,
        parsedReaction: ParsedEmojiReaction,
        targetMessage: Message,
    ) {
        targetMessage.emojiReactions = targetMessage.emojiReactions.filterNot { reaction ->
            reaction.senderPhoneNumber == reactionMessage.senderPhoneNumber &&
                reaction.emoji == parsedReaction.emoji
        }
        reactionMessage.isEmojiReaction = true
    }
}
