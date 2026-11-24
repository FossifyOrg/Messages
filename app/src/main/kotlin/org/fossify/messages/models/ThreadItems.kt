package org.fossify.messages.models

sealed class ThreadItem {
    data class ThreadMessage(
        val message: Message,
        val isGroupConversation: Boolean = false,
        val isFirstInRun: Boolean = true,
        val isLastInRun: Boolean = true
    ) : ThreadItem()

    data class ThreadDateTime(val date: Int, val simID: String) : ThreadItem()
    data class ThreadError(val messageId: Long, val messageText: String) : ThreadItem()
    data class ThreadSent(val messageId: Long, val delivered: Boolean) : ThreadItem()
    data class ThreadSending(val messageId: Long) : ThreadItem()
}

fun List<ThreadItem>.withSenderGrouping(isGroupConversation: Boolean): List<ThreadItem> {
    fun isSameSender(first: ThreadItem.ThreadMessage, second: ThreadItem?): Boolean {
        val nextMessage = (second as? ThreadItem.ThreadMessage)?.message ?: return false
        val message = first.message
        return message.isReceivedMessage() && nextMessage.isReceivedMessage() &&
            message.senderPhoneNumber.isNotBlank() && message.senderPhoneNumber == nextMessage.senderPhoneNumber
    }

    return mapIndexed { index, item ->
        if (item is ThreadItem.ThreadMessage) {
            item.copy(
                isGroupConversation = isGroupConversation,
                isFirstInRun = !isGroupConversation || !isSameSender(item, getOrNull(index - 1)),
                isLastInRun = !isGroupConversation || !isSameSender(item, getOrNull(index + 1))
            )
        } else {
            item
        }
    }
}
