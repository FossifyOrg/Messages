package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ConversationWithSnippetOverride(
    @ColumnInfo(name = "new_snippet") val snippet: String?,
    @Embedded val conversation: Conversation
) {
    fun toConversation() =
        if (snippet == null) {
            conversation
        } else {
            conversation.copy(snippet = snippet)
        }
}
