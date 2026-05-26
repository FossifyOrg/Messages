package org.fossify.messages.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.text.TextUtils
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import org.fossify.messages.dialogs.DeleteConfirmationDialog
import org.fossify.messages.dialogs.MessageDetailsDialog
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowForward
import androidx.core.content.ContextCompat
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.FeatureLockedDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.RadioItem
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.BuildConfig
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivityThreadBinding
import org.fossify.messages.dialogs.InvalidNumberDialog
import org.fossify.messages.dialogs.RenameConversationDialog
import org.fossify.messages.dialogs.ScheduleMessageDialog
import org.fossify.messages.extensions.*
import org.fossify.messages.helpers.*
import org.fossify.messages.messaging.cancelScheduleSendPendingIntent
import org.fossify.messages.messaging.isLongMmsMessage
import org.fossify.messages.messaging.isShortCodeWithLetters
import org.fossify.messages.messaging.scheduleMessage
import org.fossify.messages.messaging.sendMessageCompat
import org.fossify.messages.models.Attachment
import org.fossify.messages.models.AttachmentSelection
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Events
import org.fossify.messages.models.Message
import org.fossify.messages.models.MessageAttachment
import org.fossify.messages.models.SIMCard
import org.fossify.messages.models.ThreadItem
import org.fossify.messages.models.ThreadItem.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.joda.time.DateTime
import java.io.File
import java.util.ArrayList

class ThreadActivity : SimpleActivity() {
    private var threadId = 0L
    private var currentSIMCardIndex = 0
    private var isActivityVisible = false
    private var refreshedSinceSent = false
    private val threadItemsList = mutableStateListOf<ThreadItem>()
    private val availableSIMCards = ArrayList<SIMCard>()
    private var bus: EventBus? = null
    private var conversation: Conversation? = null
    private var participants = ArrayList<SimpleContact>()
    private var privateContacts = ArrayList<SimpleContact>()
    private var messages = ArrayList<Message>()
    private var pendingAttachmentsToSave: List<Attachment>? = null
    private var capturedImageUri: Uri? = null
    private var loadingOlderMessages = false
    private var allMessagesFetched = false
    private var isJumpingToMessage = false
    private var isRecycleBin = false
    private var isLaunchedFromShortcut = false

    private var isScheduledMessage: Boolean = false
    private var messageToResend: Long? = null
    private var scheduledMessage: Message? = null
    private lateinit var scheduledDateTime: DateTime

    // Compose state variables
    private var typeMessageText = mutableStateOf("")
    private var threadTitleState = mutableStateOf("")
    private var currentSIMCardIndexState = mutableIntStateOf(0)
    private var isScheduledMessageState = mutableStateOf(false)
    private var scheduledDateTimeTextState = mutableStateOf("")
    private val selectedMessageIds = mutableStateListOf<Long>()


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        super.finish()
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val extras = intent.extras
        if (extras == null) {
            toast(org.fossify.commons.R.string.unknown_error_occurred)
            finish()
            return
        }

        threadId = intent.getLongExtra(THREAD_ID, 0L)
        isRecycleBin = intent.getBooleanExtra(IS_RECYCLE_BIN, false)
        isLaunchedFromShortcut = intent.getBooleanExtra(IS_LAUNCHED_FROM_SHORTCUT, false)
        
        val titleStr = intent.getStringExtra(THREAD_TITLE) ?: ""
        threadTitleState.value = titleStr

        setContent {
            MessagesTheme {
                ThreadScreen()
            }
        }

        bus = EventBus.getDefault()
        bus!!.register(this)

        loadConversation()
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        notificationManager.cancel(threadId.hashCode())

        ensureBackgroundThread {
            val newConv = conversationsDB.getConversationWithThreadId(threadId)
            if (newConv != null) {
                conversation = newConv
                runOnUiThread {
                    setupThreadTitle()
                }
            }

            val smsDraft = getSmsDraft(threadId)
            if (smsDraft.isNotEmpty()) {
                runOnUiThread {
                    typeMessageText.value = smsDraft
                }
            }

            markThreadMessagesRead(threadId)
        }
    }

    override fun onPause() {
        super.onPause()
        saveDraftMessage()
        bus?.post(Events.RefreshConversations())
        isActivityVisible = false
    }

    override fun onStop() {
        super.onStop()
        saveDraftMessage()
    }

    override fun onBackPressedCompat(): Boolean {
        if (selectedMessageIds.isNotEmpty()) {
            selectedMessageIds.clear()
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    private fun saveDraftMessage() {
        val draftMessage = typeMessageText.value
        ensureBackgroundThread {
            if (draftMessage.isNotEmpty()) {
                saveSmsDraft(draftMessage, threadId)
            } else {
                deleteSmsDraft(threadId)
            }
        }
    }

    private fun setupThreadTitle() {
        val titleStr = conversation?.title ?: participants.firstOrNull()?.name ?: ""
        threadTitleState.value = titleStr
    }

    @SuppressLint("MissingPermission")
    private fun setupSIMSelector() {
        availableSIMCards.clear()
        subscriptionManagerCompat().activeSubscriptionInfoList?.forEachIndexed { index, info ->
            availableSIMCards.add(SIMCard(index + 1, info.subscriptionId, info.displayName.toString()))
        }
        currentSIMCardIndexState.value = currentSIMCardIndex
    }

    private fun updateMessageType() {
        // Automatically determine SMS vs MMS type
    }

    private fun setupCachedMessages(callback: () -> Unit) {
        ensureBackgroundThread {
            messages = try {
                if (isRecycleBin) {
                    messagesDB.getThreadMessagesFromRecycleBin(threadId)
                } else {
                    if (config.useRecycleBin) {
                        messagesDB.getNonRecycledThreadMessages(threadId)
                    } else {
                        messagesDB.getThreadMessages(threadId)
                    }
                }.toMutableList() as ArrayList<Message>
            } catch (e: Exception) {
                ArrayList()
            }
            clearExpiredScheduledMessages(threadId, messages)
            messages.removeAll { it.isScheduled && it.millis() < System.currentTimeMillis() }

            messages.sortBy { it.date }
            if (messages.size > MESSAGES_LIMIT) {
                messages = ArrayList(messages.takeLast(MESSAGES_LIMIT))
            }

            setupParticipants()
            setupAdapter()

            runOnUiThread {
                setupThreadTitle()
                setupSIMSelector()
                updateMessageType()
                callback()
            }
        }
    }

    private fun setupThread(callback: () -> Unit) {
        if (conversation == null && isLaunchedFromShortcut) {
            finish()
            return
        }
        val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
        ensureBackgroundThread {
            privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)

            val cachedMessagesCode = messages.clone().hashCode()
            if (!isRecycleBin) {
                messages = getMessages(threadId)
                if (config.useRecycleBin) {
                    val recycledMessages = messagesDB.getThreadMessagesFromRecycleBin(threadId)
                    messages = messages.filterNotInByKey(recycledMessages) { it.getStableId() }
                }
            }

            val hasParticipantWithoutName = participants.any { contact ->
                contact.phoneNumbers.map { it.normalizedNumber }.contains(contact.name)
            }

            try {
                if (participants.isNotEmpty() && messages.hashCode() == cachedMessagesCode && !hasParticipantWithoutName) {
                    setupAdapter()
                    runOnUiThread { callback() }
                    return@ensureBackgroundThread
                }
            } catch (ignored: Exception) {}

            setupParticipants()

            if (privateContacts.isNotEmpty()) {
                val senderNumbersToReplace = HashMap<String, String>()
                participants.filter { it.doesHavePhoneNumber(it.name) }.forEach { participant ->
                    privateContacts.firstOrNull { it.doesHavePhoneNumber(participant.phoneNumbers.first().normalizedNumber) }
                        ?.apply {
                            senderNumbersToReplace[participant.phoneNumbers.first().normalizedNumber] = name
                            participant.name = name
                            participant.photoUri = photoUri
                        }
                }

                messages.forEach { message ->
                    if (senderNumbersToReplace.keys.contains(message.senderName)) {
                        message.senderName = senderNumbersToReplace[message.senderName]!!
                    }
                }
            }

            if (participants.isEmpty()) {
                val name = intent.getStringExtra(THREAD_TITLE) ?: ""
                val number = intent.getStringExtra(THREAD_NUMBER)
                if (number == null) {
                    toast(org.fossify.commons.R.string.unknown_error_occurred)
                    finish()
                    return@ensureBackgroundThread
                }

                val phoneNumber = PhoneNumber(number, 0, "", number)
                val contact = SimpleContact(
                    rawId = 0,
                    contactId = 0,
                    name = name,
                    photoUri = "",
                    phoneNumbers = arrayListOf(phoneNumber),
                    birthdays = ArrayList(),
                    anniversaries = ArrayList()
                )
                participants.add(contact)
            }

            if (!isRecycleBin) {
                messages.chunked(30).forEach { currentMessages ->
                    messagesDB.insertMessages(*currentMessages.toTypedArray())
                }
            }

            setupAdapter()
            runOnUiThread {
                setupThreadTitle()
                setupSIMSelector()
                callback()
            }
        }
    }

    private fun setupAdapter() {
        val threadItems = getThreadItems()
        runOnUiThread {
            threadItemsList.clear()
            threadItemsList.addAll(threadItems.reversed())
        }
    }

    private fun handleItemClick(any: Any) {
        when {
            any is Message && any.isScheduled -> showScheduledMessageInfo(any)
            any is ThreadError -> {
                typeMessageText.value = any.messageText
                messageToResend = any.messageId
            }
        }
    }

    private fun deleteMessages(
        messagesToRemove: List<Message>,
        toRecycleBin: Boolean,
        fromRecycleBin: Boolean,
    ) {
        messages.removeAll(messagesToRemove.toSet())
        val threadItems = getThreadItems()

        runOnUiThread {
            if (messages.isEmpty()) {
                finish()
            } else {
                threadItemsList.clear()
                threadItemsList.addAll(threadItems.reversed())
            }
        }

        messagesToRemove.forEach { message ->
            val messageId = message.id
            if (message.isScheduled) {
                deleteScheduledMessage(messageId)
                cancelScheduleSendPendingIntent(messageId)
            } else {
                if (toRecycleBin) {
                    moveMessageToRecycleBin(messageId)
                } else if (fromRecycleBin) {
                    restoreMessageFromRecycleBin(messageId)
                } else {
                    deleteMessage(messageId, message.isMMS)
                }
            }
        }
        updateLastConversationMessage(threadId)

        if (messages.isNotEmpty() && messages.all { it.isScheduled }) {
            val scheduledMessage = messages.last()
            val fakeThreadId = generateRandomId()
            createTemporaryThread(scheduledMessage, fakeThreadId, conversation)
            updateScheduledMessagesThreadId(messages, fakeThreadId)
            threadId = fakeThreadId
        }
    }

    private fun loadConversation() {
        handlePermission(PERMISSION_READ_PHONE_STATE) { granted ->
            if (granted) {
                setupCachedMessages {
                    setupThread {}
                }
            } else {
                finish()
            }
        }
    }

    private fun setupParticipants() {
        if (isFinishing) return
        if (threadId != 0L) {
            participants = getThreadParticipants(threadId, null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getThreadItems(): ArrayList<ThreadItem> {
        val items = ArrayList<ThreadItem>()
        if (isFinishing) return items

        messages.sortBy { it.date }

        val subscriptionIdToSimId = HashMap<Int, String>()
        subscriptionIdToSimId[-1] = "?"
        subscriptionManagerCompat().activeSubscriptionInfoList?.forEachIndexed { index, info ->
            subscriptionIdToSimId[info.subscriptionId] = "${index + 1}"
        }

        var prevDateTime = 0
        var prevSIMId = -2
        var hadUnreadItems = false
        val cnt = messages.size
        for (i in 0 until cnt) {
            val message = messages.getOrNull(i) ?: continue
            val isSentFromDifferentKnownSIM =
                prevSIMId != -1 && message.subscriptionId != -1 && prevSIMId != message.subscriptionId
            if (message.date - prevDateTime > MIN_DATE_TIME_DIFF_SECS || isSentFromDifferentKnownSIM) {
                val simCardID = subscriptionIdToSimId[message.subscriptionId] ?: "?"
                items.add(ThreadDateTime(message.date, simCardID))
                prevDateTime = message.date
            }
            items.add(message)

            if (message.type == Telephony.Sms.MESSAGE_TYPE_FAILED) {
                items.add(ThreadError(message.id, message.body))
            }

            if (message.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX) {
                items.add(ThreadSending(message.id))
            }

            if (!message.read) {
                hadUnreadItems = true
                markMessageRead(message.id, message.isMMS)
                conversationsDB.markRead(threadId)
            }

            if (i == cnt - 1 && (message.type == Telephony.Sms.MESSAGE_TYPE_SENT)) {
                items.add(ThreadSent(messageId = message.id, delivered = message.status == Telephony.Sms.STATUS_COMPLETE))
            }
            prevSIMId = message.subscriptionId
        }

        if (hadUnreadItems) {
            bus?.post(Events.RefreshConversations())
        }

        return items
    }

    private fun sendMessage() {
        var text = typeMessageText.value
        if (text.isEmpty()) {
            showErrorToast(getString(org.fossify.commons.R.string.unknown_error_occurred))
            return
        }

        text = removeDiacriticsIfNeeded(text)

        val subscriptionId = availableSIMCards.getOrNull(currentSIMCardIndexState.value)?.subscriptionId
            ?: SmsManager.getDefaultSmsSubscriptionId()

        if (isScheduledMessageState.value) {
            sendScheduledMessage(text, subscriptionId)
        } else {
            sendNormalMessage(text, subscriptionId)
        }
    }

    private fun sendScheduledMessage(text: String, subscriptionId: Int) {
        if (scheduledDateTime.millis < System.currentTimeMillis() + 1000L) {
            toast(R.string.must_pick_time_in_the_future)
            launchScheduleSendDialog(scheduledDateTime)
            return
        }

        refreshedSinceSent = false
        try {
            ensureBackgroundThread {
                val messageId = scheduledMessage?.id ?: generateRandomId()
                val message = buildScheduledMessage(text, subscriptionId, messageId)
                if (messages.isEmpty()) {
                    threadId = message.threadId
                    createTemporaryThread(message, message.threadId, conversation)
                }
                val conversation = conversationsDB.getConversationWithThreadId(threadId)
                if (conversation != null) {
                    val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
                    conversationsDB.insertOrUpdate(
                        conversation.copy(date = nowSeconds, snippet = message.body)
                    )
                }
                scheduleMessage(message)
                insertOrUpdateMessage(message)

                runOnUiThread {
                    typeMessageText.value = ""
                    isScheduledMessageState.value = false
                    scheduledMessage = null
                }
            }
        } catch (e: Exception) {
            showErrorToast(e.localizedMessage ?: getString(org.fossify.commons.R.string.unknown_error_occurred))
        }
    }

    private fun sendNormalMessage(text: String, subscriptionId: Int) {
        val addresses = participants.getAddresses()
        val attachments = emptyList<Attachment>()

        try {
            refreshedSinceSent = false
            sendMessageCompat(text, addresses, subscriptionId, attachments, messageToResend)
            ensureBackgroundThread {
                val messages = getMessages(threadId, limit = maxOf(1, attachments.size))
                    .filterNotInByKey(messages) { it.getStableId() }
                for (message in messages) {
                    insertOrUpdateMessage(message)
                }

                runOnUiThread {
                    typeMessageText.value = ""
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun insertOrUpdateMessage(message: Message) {
        messagesDB.insertOrUpdate(message)
        updateLastConversationMessage(threadId)
        setupAdapter()
    }

    private fun showScheduledMessageInfo(message: Message) {
        // Stub for schedule message popup details
    }

    private fun launchScheduleSendDialog(dateTime: DateTime) {
        ScheduleMessageDialog(this, dateTime) { pickedDateTime ->
            if (pickedDateTime != null) {
                scheduledDateTime = pickedDateTime
                isScheduledMessageState.value = true
                scheduledDateTimeTextState.value = pickedDateTime.toString("dd MMM yyyy, HH:mm")
            }
        }
    }

    private fun isSpecialNumber() = participants.size == 1 && isShortCodeWithLetters(participants.first().name)

    private fun tryBlocking() {
        if (isOrWasThankYouInstalled()) {
            askConfirmBlock()
        } else {
            FeatureLockedDialog(this) { }
        }
    }

    private fun askConfirmBlock() {
        val numbers = participants.distinctBy { it.phoneNumbers.firstOrNull()?.normalizedNumber }.map { it.name }
        val numbersString = TextUtils.join(", ", numbers)
        val question = String.format(resources.getString(org.fossify.commons.R.string.block_confirmation), numbersString)
        ConfirmationDialog(this, question) {
            blockNumbers()
        }
    }

    private fun blockNumbers() {
        ensureBackgroundThread {
            participants.mapNotNull { it.phoneNumbers.firstOrNull()?.normalizedNumber }.forEach { number ->
                addBlockedNumber(number)
            }
            runOnUiThread {
                finish()
            }
        }
    }

    private fun askConfirmDelete() {
        val itemsCnt = messages.size
        val items = resources.getQuantityString(R.plurals.delete_conversations, itemsCnt, itemsCnt)
        val baseString = org.fossify.commons.R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)
        ConfirmationDialog(this, question) {
            ensureBackgroundThread {
                deleteConversation(threadId)
                runOnUiThread { finish() }
            }
        }
    }

    private fun archiveConversation() {
        ensureBackgroundThread {
            updateConversationArchivedStatus(threadId, true)
            runOnUiThread { finish() }
        }
    }

    private fun unarchiveConversation() {
        ensureBackgroundThread {
            updateConversationArchivedStatus(threadId, false)
            runOnUiThread { finish() }
        }
    }

    private fun renameConversation() {
        conversation?.let {
            RenameConversationDialog(this, it) { newTitle ->
                ensureBackgroundThread {
                    renameConversation(it, newTitle)
                    runOnUiThread {
                        threadTitleState.value = newTitle
                    }
                }
            }
        }
    }

    private fun addNumberToContact() {
        val firstPhoneNumber = participants.firstOrNull()?.phoneNumbers?.firstOrNull()?.value ?: return
        Intent().apply {
            action = Intent.ACTION_INSERT_OR_EDIT
            type = "vnd.android.cursor.item/contact"
            putExtra(KEY_PHONE, firstPhoneNumber)
            launchActivityIntent(this)
        }
    }

    private fun copyNumberToClipboard() {
        val firstPhoneNumber = participants.firstOrNull()?.phoneNumbers?.firstOrNull()?.value ?: return
        copyToClipboard(firstPhoneNumber)
    }

    private fun dialNumber() {
        val firstPhoneNumber = participants.firstOrNull()?.phoneNumbers?.firstOrNull()?.value ?: return
        dialNumber(firstPhoneNumber) {}
    }

    private fun managePeople() {}

    private fun markAsUnread() {
        ensureBackgroundThread {
            markThreadMessagesUnread(threadId)
            runOnUiThread { finish() }
        }
    }

    private fun askConfirmRestoreAll() {
        ensureBackgroundThread {
            restoreAllMessagesFromRecycleBinForConversation(threadId)
            runOnUiThread { finish() }
        }
    }

    private fun addAttachment(uri: Uri) {}
    private fun addContactAttachment(uri: Uri) {}
    private fun saveAttachments(intent: Intent?) {}
    private fun getAttachmentSelections(): List<AttachmentSelection> = emptyList()
    private fun buildMessageAttachments(): List<MessageAttachment> = emptyList()
    private fun buildScheduledMessage(text: String, subscriptionId: Int, messageId: Long) = Message(
        id = messageId,
        body = text,
        type = Telephony.Sms.MESSAGE_TYPE_QUEUED,
        status = Telephony.Sms.STATUS_NONE,
        participants = participants,
        date = (scheduledDateTime.millis / 1000).toInt(),
        read = true,
        threadId = threadId,
        isMMS = false,
        attachment = null,
        senderPhoneNumber = "",
        senderName = "",
        senderPhotoUri = "",
        subscriptionId = subscriptionId,
        isScheduled = true
    )

    fun saveMMS(attachments: List<Attachment>) {
        pendingAttachmentsToSave = attachments
        if (attachments.size == 1) {
            val attachment = attachments.first()
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = attachment.mimetype
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, attachment.uriString.split("/").last())
                launchActivityForResult(
                    intent = this,
                    requestCode = PICK_SAVE_FILE_INTENT,
                    error = org.fossify.commons.R.string.system_service_disabled
                )
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                launchActivityForResult(
                    intent = this,
                    requestCode = PICK_SAVE_DIR_INTENT,
                    error = org.fossify.commons.R.string.system_service_disabled
                )
            }
        }
    }

    private fun launchActivityForResult(
        intent: Intent,
        requestCode: Int,
        @androidx.annotation.StringRes error: Int = org.fossify.commons.R.string.no_app_found
    ) {
        hideKeyboard()
        try {
            startActivityForResult(intent, requestCode)
        } catch (e: ActivityNotFoundException) {
            showErrorToast(getString(error))
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(@Suppress("unused") event: Events.RefreshMessages) {
        loadConversation()
    }

    // -------------------------------------------------------------
    // Compose Chat Interface Implementation (Jetpack Compose M3)
    // -------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun ThreadScreen() {
        val scrollState = rememberLazyListState()
        var showMenuDropdown by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                if (selectedMessageIds.isNotEmpty()) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "${selectedMessageIds.size} selected",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedMessageIds.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear selection")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                val selectedMsgs = messages.filter { selectedMessageIds.contains(it.id) }
                                val textToCopy = if (selectedMsgs.size == 1) {
                                    selectedMsgs.first().body
                                } else {
                                    selectedMsgs.filter { it.body.isNotEmpty() }.joinToString("\n\n") { message ->
                                        val format = "${config.dateFormat}, ${getTimeFormat()}"
                                        val dateTime = DateTime(message.millis()).toString(format)
                                        val sender = if (message.isReceivedMessage()) message.senderName else getString(R.string.me)
                                        "[$dateTime] $sender: ${message.body}"
                                    }
                                }
                                if (textToCopy.isNotEmpty()) {
                                    copyToClipboard(textToCopy)
                                }
                                selectedMessageIds.clear()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard")
                            }

                            if (selectedMessageIds.size == 1) {
                                val selectedMsg = messages.find { it.id == selectedMessageIds.first() }
                                if (selectedMsg != null && selectedMsg.body.isNotEmpty()) {
                                    IconButton(onClick = {
                                        shareTextIntent(selectedMsg.body)
                                        selectedMessageIds.clear()
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share")
                                    }
                                }
                            }

                            if (selectedMessageIds.size == 1) {
                                val selectedMsg = messages.find { it.id == selectedMessageIds.first() }
                                if (selectedMsg != null) {
                                    IconButton(onClick = {
                                        val attachment = selectedMsg.attachment?.attachments?.firstOrNull()
                                        Intent(this@ThreadActivity, NewConversationActivity::class.java).apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, selectedMsg.body)
                                            if (attachment != null) {
                                                putExtra(Intent.EXTRA_STREAM, attachment.getUri())
                                            }
                                            startActivity(this)
                                        }
                                        selectedMessageIds.clear()
                                    }) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                                    }
                                }
                            }

                            if (selectedMessageIds.size == 1) {
                                val selectedMsg = messages.find { it.id == selectedMessageIds.first() }
                                if (selectedMsg != null) {
                                    IconButton(onClick = {
                                        MessageDetailsDialog(this@ThreadActivity, selectedMsg)
                                        selectedMessageIds.clear()
                                    }) {
                                        Icon(Icons.Default.Info, contentDescription = "Details")
                                    }
                                }
                            }

                            if (isRecycleBin) {
                                IconButton(onClick = {
                                    val selectedMsgs = messages.filter { selectedMessageIds.contains(it.id) }
                                    val itemsCnt = selectedMsgs.size
                                    val items = try {
                                        resources.getQuantityString(R.plurals.delete_messages, itemsCnt, itemsCnt)
                                    } catch (e: Exception) {
                                        showErrorToast(e)
                                        return@IconButton
                                    }
                                    val question = String.format(resources.getString(R.string.restore_confirmation), items)
                                    ConfirmationDialog(this@ThreadActivity, question) {
                                        ensureBackgroundThread {
                                            deleteMessages(selectedMsgs, false, true)
                                            runOnUiThread {
                                                selectedMessageIds.clear()
                                            }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Restore")
                                }
                            }

                            IconButton(onClick = {
                                val selectedMsgs = messages.filter { selectedMessageIds.contains(it.id) }
                                val itemsCnt = selectedMsgs.size
                                val items = try {
                                    resources.getQuantityString(R.plurals.delete_messages, itemsCnt, itemsCnt)
                                } catch (e: Exception) {
                                    showErrorToast(e)
                                    return@IconButton
                                }
                                val baseString = if (config.useRecycleBin && !isRecycleBin) {
                                    org.fossify.commons.R.string.move_to_recycle_bin_confirmation
                                } else {
                                    org.fossify.commons.R.string.deletion_confirmation
                                }
                                val question = String.format(resources.getString(baseString), items)
                                DeleteConfirmationDialog(this@ThreadActivity, question, config.useRecycleBin && !isRecycleBin) { skipRecycleBin ->
                                    ensureBackgroundThread {
                                        val toRecycleBin = !skipRecycleBin && config.useRecycleBin && !isRecycleBin
                                        deleteMessages(selectedMsgs, toRecycleBin, false)
                                        runOnUiThread {
                                            selectedMessageIds.clear()
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = threadTitleState.value,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (participants.size == 1 && !isSpecialNumber()) {
                                    Text(
                                        text = participants.first().phoneNumbers.firstOrNull()?.value ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (participants.size == 1 && !isSpecialNumber() && !isRecycleBin) {
                                IconButton(onClick = { dialNumber() }) {
                                    Icon(Icons.Default.Call, contentDescription = "Call")
                                }
                            }
                            IconButton(onClick = { showMenuDropdown = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }

                            DropdownMenu(
                                expanded = showMenuDropdown,
                                onDismissRequest = { showMenuDropdown = false }
                            ) {
                                if (threadItemsList.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Conversation") },
                                        onClick = {
                                            showMenuDropdown = false
                                            askConfirmDelete()
                                        }
                                    )
                                }
                                if (conversation?.isArchived == false && !isRecycleBin) {
                                    DropdownMenuItem(
                                        text = { Text("Archive") },
                                        onClick = {
                                            showMenuDropdown = false
                                            archiveConversation()
                                        }
                                    )
                                }
                                if (conversation?.isArchived == true && !isRecycleBin) {
                                    DropdownMenuItem(
                                        text = { Text("Unarchive") },
                                        onClick = {
                                            showMenuDropdown = false
                                            unarchiveConversation()
                                        }
                                    )
                                }
                                if (!isRecycleBin) {
                                    DropdownMenuItem(
                                        text = { Text("Block Sender") },
                                        onClick = {
                                            showMenuDropdown = false
                                            tryBlocking()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            },
            bottomBar = {
                if (!isRecycleBin) {
                    ChatInputBar()
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (threadItemsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        reverseLayout = true
                    ) {
                        itemsIndexed(threadItemsList.toList()) { index, item ->
                            when (item) {
                                is ThreadDateTime -> DateTimeDivider(item)
                                is Message -> ChatBubble(item)
                                is ThreadError -> Text(
                                    text = "Error sending message",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                is ThreadSending -> Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                is ThreadSent -> Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DateTimeDivider(divider: ThreadDateTime) {
        val dateText = remember(divider.date) {
            val millis = divider.date * 1000L
            android.text.format.DateUtils.formatDateTime(
                this@ThreadActivity,
                millis,
                android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_NO_YEAR or android.text.format.DateUtils.FORMAT_SHOW_TIME
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ChatBubble(message: Message) {
        val isSent = remember(message.type) {
            message.type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                    message.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                    message.type == Telephony.Sms.MESSAGE_TYPE_FAILED
        }

        val isSelected = selectedMessageIds.contains(message.id)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else Color.Transparent
                )
                .combinedClickable(
                    onLongClick = {
                        if (selectedMessageIds.contains(message.id)) {
                            selectedMessageIds.remove(message.id)
                        } else {
                            selectedMessageIds.add(message.id)
                        }
                    },
                    onClick = {
                        if (selectedMessageIds.isNotEmpty()) {
                            if (selectedMessageIds.contains(message.id)) {
                                selectedMessageIds.remove(message.id)
                            } else {
                                selectedMessageIds.add(message.id)
                            }
                        } else {
                            handleItemClick(message)
                        }
                    }
                )
                .padding(vertical = 4.dp, horizontal = 12.dp),
            contentAlignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSent) 16.dp else 4.dp,
                            bottomEnd = if (isSent) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isSent) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    Text(
                        text = message.body,
                        fontSize = 15.sp,
                        color = if (isSent) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val timeStr = remember(message.date) {
                        (message.date * 1000L).formatDateOrTime(
                            context = this@ThreadActivity,
                            hideTimeOnOtherDays = true,
                            showCurrentYear = false
                        )
                    }

                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }

    @Composable
    private fun ChatInputBar() {
        var textVal by remember { mutableStateOf(TextFieldValue(typeMessageText.value)) }

        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Scheduled message banner
                if (isScheduledMessageState.value) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scheduled: ${scheduledDateTimeTextState.value}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = {
                                isScheduledMessageState.value = false
                                scheduledMessage = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // SIM Switch icon if multiple SIMs exist
                    if (availableSIMCards.size > 1) {
                        IconButton(
                            onClick = {
                                val nextIndex = (currentSIMCardIndexState.value + 1) % availableSIMCards.size
                                currentSIMCardIndexState.value = nextIndex
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painterResource(org.fossify.commons.R.drawable.ic_sim_vector),
                                    contentDescription = "Select SIM",
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = (currentSIMCardIndexState.value + 1).toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Plus icon for Attachment/Schedule Options
                    IconButton(
                        onClick = {
                            val now = DateTime.now().plusHours(1)
                            launchScheduleSendDialog(now)
                        }
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Attachment Options",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Multiline expanding TextField
                    OutlinedTextField(
                        value = textVal,
                        onValueChange = {
                            textVal = it
                            typeMessageText.value = it.text
                        },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 120.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            sendMessage()
                            textVal = TextFieldValue("")
                        },
                        enabled = textVal.text.trim().isNotEmpty(),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (textVal.text.trim().isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            painterResource(org.fossify.commons.R.drawable.ic_send_vector),
                            contentDescription = "Send",
                            tint = if (textVal.text.trim().isNotEmpty()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    companion object {
        private const val TYPE_EDIT = 14
        private const val TYPE_SEND = 15
        private const val TYPE_DELETE = 16
        private const val MIN_DATE_TIME_DIFF_SECS = 300
        private const val SCROLL_TO_BOTTOM_FAB_LIMIT = 20
        private const val PREFETCH_THRESHOLD = 45
    }
}
