package org.fossify.messages.activities

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.text.TextUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.FAQItem
import org.fossify.commons.models.Release
import org.fossify.messages.BuildConfig
import org.fossify.messages.R
import org.fossify.messages.extensions.*
import org.fossify.messages.helpers.*
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Events
import org.fossify.messages.models.Message
import org.fossify.messages.models.SearchResult
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.Locale

class MainActivity : SimpleActivity() {
    override var isSearchBarEnabled = true
    
    private val MAKE_DEFAULT_APP_REQUEST = 1

    enum class Tab {
        PERSONAL, FINANCIAL, OTHERS
    }

    enum class ConversationFilter {
        ALL, UNREAD, FAVOURITES, GROUP
    }

    private var allLoadedConversations = ArrayList<Conversation>()
    private val conversationsStateList = mutableStateListOf<Conversation>()
    private var isProgressLoading = mutableStateOf(false)
    private var lastSearchedText = mutableStateOf("")
    private val searchResultsStateList = mutableStateListOf<SearchResult>()
    private var isSearchOpen = mutableStateOf(false)
    private val selectedThreadIds = mutableStateListOf<Long>()

    private var storedTextColor = 0
    private var storedFontSize = 0
    private var bus: EventBus? = null

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appLaunched(BuildConfig.APPLICATION_ID)

        setContent {
            MessagesTheme {
                MainScreen()
            }
        }

        checkAndDeleteOldRecycleBinMessages()
        clearAllMessagesIfNeeded {
            loadMessages()
        }
    }

    override fun onResume() {
        super.onResume()
        checkShortcut()
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (selectedThreadIds.isNotEmpty()) {
            selectedThreadIds.clear()
        } else if (isSearchOpen.value) {
            isSearchOpen.value = false
            lastSearchedText.value = ""
        } else {
            super.onBackPressed()
        }
    }

    private fun storeStateVariables() {
        storedTextColor = getProperTextColor()
        storedFontSize = config.fontSize
    }

    private fun loadMessages() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    askPermissions()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            } else {
                toast(org.fossify.commons.R.string.unknown_error_occurred)
                finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                askPermissions()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
            }
        }
    }

    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            handleNotificationPermission { granted ->
                                if (!granted) {
                                    PermissionRequiredDialog(
                                        activity = this,
                                        textId = org.fossify.commons.R.string.allow_notifications_incoming_messages,
                                        positiveActionCallback = { openNotificationSettings() })
                                }
                            }

                            initMessenger()
                        }
                    } else {
                        finish()
                    }
                }
            } else {
                finish()
            }
        }
    }

    private fun initMessenger() {
        checkWhatsNewDialog()
        storeStateVariables()
        getCachedConversations()
    }

    private fun getCachedConversations() {
        ensureBackgroundThread {
            val conversations = try {
                conversationsDB.getNonArchived().toMutableList() as ArrayList<Conversation>
            } catch (_: Exception) {
                ArrayList()
            }

            val archived = try {
                conversationsDB.getAllArchived()
            } catch (_: Exception) {
                listOf()
            }

            runOnUiThread {
                setupConversations(conversations, cached = true)
            }
            getNewConversations(
                (conversations + archived).toMutableList() as ArrayList<Conversation>
            )
            conversations.forEach {
                clearExpiredScheduledMessages(it.threadId)
            }
        }
    }

    private fun getNewConversations(cachedConversations: ArrayList<Conversation>) {
        ensureBackgroundThread {
            val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
            val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
            val conversations = getConversations(privateContacts = privateContacts)

            conversations.forEach { clonedConversation ->
                val threadIds = cachedConversations.map { it.threadId }
                if (!threadIds.contains(clonedConversation.threadId)) {
                    conversationsDB.insertOrUpdate(clonedConversation)
                    cachedConversations.add(clonedConversation)
                }
            }

            cachedConversations.forEach { cachedConversation ->
                val threadId = cachedConversation.threadId

                val isTemporaryThread = cachedConversation.isScheduled
                val isConversationDeleted = !conversations.map { it.threadId }.contains(threadId)
                if (isConversationDeleted && !isTemporaryThread) {
                    conversationsDB.deleteThreadId(threadId)
                }

                val newConversation =
                    conversations.find { it.phoneNumber == cachedConversation.phoneNumber }
                if (isTemporaryThread && newConversation != null) {
                    conversationsDB.deleteThreadId(threadId)
                    messagesDB.getScheduledThreadMessages(threadId)
                        .forEach { message ->
                            messagesDB.insertOrUpdate(
                                message.copy(threadId = newConversation.threadId)
                            )
                        }
                    insertOrUpdateConversation(newConversation, cachedConversation)
                }
            }

            cachedConversations.forEach { cachedConv ->
                val conv = conversations.find {
                    it.threadId == cachedConv.threadId && !Conversation.areContentsTheSame(
                        old = cachedConv, new = it
                    )
                }
                if (conv != null) {
                    insertOrUpdateConversation(conv)
                }
            }

            val allConversations = conversationsDB.getNonArchived() as ArrayList<Conversation>
            runOnUiThread {
                setupConversations(allConversations)
            }

            if (config.appRunCount == 1) {
                conversations.map { it.threadId }.forEach { threadId ->
                    val messages = getMessages(threadId, includeScheduledMessages = false)
                    messages.chunked(30).forEach { currentMessages ->
                        messagesDB.insertMessages(*currentMessages.toTypedArray())
                    }
                }
            }
        }
    }

    private fun setupConversations(
        conversations: ArrayList<Conversation>,
        cached: Boolean = false,
    ) {
        allLoadedConversations = conversations
        runOnUiThread {
            conversationsStateList.clear()
            conversationsStateList.addAll(conversations)
            
            if (cached && config.appRunCount == 1) {
                isProgressLoading.value = conversations.isEmpty()
            } else {
                isProgressLoading.value = false
            }
        }
    }

    private fun searchTextChanged(text: String) {
        lastSearchedText.value = text
        if (text.length >= 2) {
            ensureBackgroundThread {
                val searchQuery = "%$text%"
                val messages = messagesDB.getMessagesWithText(searchQuery)
                val conversations = conversationsDB.getConversationsWithText(searchQuery)
                if (text == lastSearchedText.value) {
                    showSearchResults(messages, conversations, text)
                }
            }
        } else {
            runOnUiThread {
                searchResultsStateList.clear()
            }
        }
    }

    private fun showSearchResults(
        messages: List<Message>,
        conversations: List<Conversation>,
        searchedText: String,
    ) {
        val searchResults = ArrayList<SearchResult>()
        conversations.forEach { conversation ->
            val date = (conversation.date * 1000L).formatDateOrTime(
                context = this,
                hideTimeOnOtherDays = true,
                showCurrentYear = true
            )

            val searchResult = SearchResult(
                messageId = -1,
                title = conversation.title,
                snippet = conversation.phoneNumber,
                date = date,
                threadId = conversation.threadId,
                photoUri = conversation.photoUri
            )
            searchResults.add(searchResult)
        }

        messages.sortedByDescending { it.id }.forEach { message ->
            var recipient = message.senderName
            if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                val participantNames = message.participants.map { it.name }
                recipient = TextUtils.join(", ", participantNames)
            }

            val date = (message.date * 1000L).formatDateOrTime(
                context = this,
                hideTimeOnOtherDays = true,
                showCurrentYear = true
            )

            val searchResult = SearchResult(
                messageId = message.id,
                title = recipient,
                snippet = message.body,
                date = date,
                threadId = message.threadId,
                photoUri = message.senderPhotoUri
            )
            searchResults.add(searchResult)
        }

        runOnUiThread {
            searchResultsStateList.clear()
            searchResultsStateList.addAll(searchResults)
        }
    }

    private fun handleConversationClick(conversation: Conversation) {
        Intent(this, ThreadActivity::class.java).apply {
            putExtra(THREAD_ID, conversation.threadId)
            putExtra(THREAD_TITLE, conversation.title)
            startActivity(this)
        }
    }

    private fun launchNewConversation() {
        Intent(this, NewConversationActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun launchRecycleBin() {
        startActivity(Intent(applicationContext, RecycleBinConversationsActivity::class.java))
    }

    private fun launchArchivedConversations() {
        startActivity(Intent(applicationContext, ArchivedConversationsActivity::class.java))
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_EVENT_BUS or LICENSE_SMS_MMS or LICENSE_INDICATOR_FAST_SCROLL

        val faqItems = arrayListOf(
            FAQItem(title = R.string.faq_2_title, text = R.string.faq_2_text),
            FAQItem(title = R.string.faq_3_title, text = R.string.faq_3_text),
            FAQItem(title = R.string.faq_4_title, text = R.string.faq_4_text),
            FAQItem(
                title = org.fossify.commons.R.string.faq_9_title_commons,
                text = org.fossify.commons.R.string.faq_9_text_commons
            )
        )

        if (!resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)) {
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_2_title_commons,
                    text = org.fossify.commons.R.string.faq_2_text_commons
                )
            )
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_6_title_commons,
                    text = org.fossify.commons.R.string.faq_6_text_commons
                )
            )
        }

        startAboutActivity(
            appNameId = R.string.app_name,
            licenseMask = licenses,
            versionName = BuildConfig.VERSION_NAME,
            faqItems = faqItems,
            showFAQBeforeMail = true
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
        initMessenger()
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }

    private fun isFinancial(conversation: Conversation): Boolean {
        val text = (conversation.snippet + " " + conversation.title).lowercase()
        val financialKeywords = listOf(
            "bank", "account", "txn", "transaction", "debit", "credit", "otp", 
            "spent", "withdraw", "rs.", "inr", "paytm", "gpay", "card", "payment"
        )
        return financialKeywords.any { text.contains(it) }
    }

    private fun isPersonal(conversation: Conversation): Boolean {
        if (isFinancial(conversation)) return false
        
        val number = conversation.phoneNumber.trim()
        if (number.isEmpty()) return false
        
        if (conversation.title != conversation.phoneNumber) {
            return true
        }
        
        val digitsAndSymbols = number.filter { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' || it.isWhitespace() }
        if (digitsAndSymbols.length != number.length) {
            return false
        }
        
        val digitCount = number.filter { it.isDigit() }.length
        if (digitCount < 7) {
            return false
        }
        
        return true
    }

    @SuppressLint("NewApi")
    private fun checkShortcut() {
        val appIconColor = config.appIconColor
        if (config.lastHandledShortcutColor != appIconColor) {
            val newConversation = getCreateNewContactShortcut(appIconColor)

            val manager = getSystemService(ShortcutManager::class.java)
            try {
                manager.dynamicShortcuts = listOf(newConversation)
                config.lastHandledShortcutColor = appIconColor
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("NewApi")
    private fun getCreateNewContactShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_conversation)
        val drawable =
            ContextCompat.getDrawable(this, org.fossify.commons.R.drawable.shortcut_plus)

        (drawable as LayerDrawable).findDrawableByLayerId(
            org.fossify.commons.R.id.shortcut_plus_background
        ).applyColorFilter(appIconColor)

        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, NewConversationActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        return ShortcutInfo.Builder(this, "new_conversation")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .setRank(0)
            .build()
    }

    // -------------------------------------------------------------
    // Compose User Interface Implementation (Jetpack Compose M3)
    // -------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen() {
        var currentTab by remember { mutableStateOf(Tab.PERSONAL) }
        var currentFilter by remember { mutableStateOf(ConversationFilter.ALL) }
        var isMenuSheetOpen by remember { mutableStateOf(false) }

        BackHandler(enabled = selectedThreadIds.isNotEmpty() || isSearchOpen.value) {
            if (selectedThreadIds.isNotEmpty()) {
                selectedThreadIds.clear()
            } else {
                isSearchOpen.value = false
                lastSearchedText.value = ""
            }
        }

        // Compute list dynamically
        val sortedConversations = remember {
            derivedStateOf {
                val pinned = config.pinnedConversations
                conversationsStateList.sortedWith(
                    compareByDescending<Conversation> {
                        pinned.contains(it.threadId.toString())
                    }.thenByDescending { it.date }
                )
            }
        }

        val tabFilteredList = remember {
            derivedStateOf {
                val list = sortedConversations.value
                when (currentTab) {
                    Tab.PERSONAL -> list.filter { isPersonal(it) }
                    Tab.FINANCIAL -> list.filter { isFinancial(it) }
                    Tab.OTHERS -> list.filter { !isPersonal(it) && !isFinancial(it) }
                }
            }
        }

        val finalList = remember {
            derivedStateOf {
                val list = tabFilteredList.value
                val pinned = config.pinnedConversations
                when (currentFilter) {
                    ConversationFilter.ALL -> list
                    ConversationFilter.UNREAD -> list.filter { !it.read || it.unreadCount > 0 }
                    ConversationFilter.FAVOURITES -> list.filter { pinned.contains(it.threadId.toString()) }
                    ConversationFilter.GROUP -> list.filter { it.isGroupConversation }
                }
            }
        }

        Scaffold(
            topBar = {
                if (selectedThreadIds.isEmpty()) {
                    MainSearchBar()
                } else {
                    SelectionTopBar(
                        selectedIds = selectedThreadIds,
                        onClose = { selectedThreadIds.clear() },
                        onDelete = {
                            val selectedConversations = conversationsStateList.filter { selectedThreadIds.contains(it.threadId) }
                            askConfirmDelete(selectedConversations) {
                                deleteThreads(selectedThreadIds.toList())
                                selectedThreadIds.clear()
                            }
                        },
                        onArchive = {
                            val selectedConversations = conversationsStateList.filter { selectedThreadIds.contains(it.threadId) }
                            askConfirmArchive(selectedConversations) {
                                archiveThreads(selectedThreadIds.toList())
                                selectedThreadIds.clear()
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == Tab.PERSONAL,
                        onClick = { 
                            currentTab = Tab.PERSONAL 
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = getString(R.string.tab_personal)) },
                        label = { Text(getString(R.string.tab_personal), fontWeight = if (currentTab == Tab.PERSONAL) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == Tab.FINANCIAL,
                        onClick = { 
                            currentTab = Tab.FINANCIAL
                            if (currentFilter == ConversationFilter.FAVOURITES || currentFilter == ConversationFilter.GROUP) {
                                currentFilter = ConversationFilter.ALL
                            }
                        },
                        icon = { Icon(painterResource(R.drawable.ic_tab_financial), contentDescription = getString(R.string.tab_financial), modifier = Modifier.size(24.dp)) },
                        label = { Text(getString(R.string.tab_financial), fontWeight = if (currentTab == Tab.FINANCIAL) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = currentTab == Tab.OTHERS,
                        onClick = { 
                            currentTab = Tab.OTHERS
                            if (currentFilter == ConversationFilter.FAVOURITES || currentFilter == ConversationFilter.GROUP) {
                                currentFilter = ConversationFilter.ALL
                            }
                        },
                        icon = { Icon(painterResource(R.drawable.ic_tab_others), contentDescription = getString(R.string.tab_others), modifier = Modifier.size(24.dp)) },
                        label = { Text(getString(R.string.tab_others), fontWeight = if (currentTab == Tab.OTHERS) FontWeight.Bold else FontWeight.Normal) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { isMenuSheetOpen = true },
                        icon = { Icon(Icons.Default.Menu, contentDescription = getString(R.string.menu)) },
                        label = { Text(getString(R.string.menu)) }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { launchNewConversation() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = getString(R.string.new_conversation))
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Category filters
                CategoryFilterRow(
                    currentTab = currentTab,
                    selectedFilter = currentFilter,
                    onFilterSelected = { currentFilter = it }
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProgressLoading.value) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (finalList.value.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = getString(R.string.no_conversations_found),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { launchNewConversation() }) {
                                Text(getString(R.string.start_conversation))
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(finalList.value, key = { it.threadId }) { conversation ->
                                ConversationListItem(
                                    conversation = conversation,
                                    isSelected = selectedThreadIds.contains(conversation.threadId),
                                    onClick = {
                                        if (selectedThreadIds.isNotEmpty()) {
                                            if (selectedThreadIds.contains(conversation.threadId)) {
                                                selectedThreadIds.remove(conversation.threadId)
                                            } else {
                                                selectedThreadIds.add(conversation.threadId)
                                            }
                                        } else {
                                            handleConversationClick(conversation)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectedThreadIds.contains(conversation.threadId)) {
                                            selectedThreadIds.add(conversation.threadId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom sheet menu trigger
        if (isMenuSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isMenuSheetOpen = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(getString(org.fossify.commons.R.string.settings)) },
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                        modifier = Modifier.clickable {
                            isMenuSheetOpen = false
                            launchSettings()
                        }
                    )
                    if (config.isArchiveAvailable) {
                        ListItem(
                            headlineContent = { Text(getString(R.string.archived_conversations)) },
                            leadingContent = { Icon(Icons.Default.MailOutline, contentDescription = null) },
                            modifier = Modifier.clickable {
                                isMenuSheetOpen = false
                                launchArchivedConversations()
                            }
                        )
                    }
                    if (config.useRecycleBin) {
                        ListItem(
                            headlineContent = { Text(getString(org.fossify.commons.R.string.recycle_bin)) },
                            leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                            modifier = Modifier.clickable {
                                isMenuSheetOpen = false
                                launchRecycleBin()
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text(getString(org.fossify.commons.R.string.about)) },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.clickable {
                            isMenuSheetOpen = false
                            launchAbout()
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainSearchBar() {
        var query by remember { mutableStateOf("") }
        val searchBarState = rememberSearchBarState()
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val isExpanded = searchBarState.targetValue == SearchBarValue.Expanded

        // Sync external isSearchOpen with searchBarState
        LaunchedEffect(isExpanded) {
            isSearchOpen.value = isExpanded
            if (!isExpanded) {
                query = ""
                lastSearchedText.value = ""
                searchResultsStateList.clear()
            }
        }

        // Handle external requests to close search (e.g. from Activity.onBackPressed)
        LaunchedEffect(isSearchOpen.value) {
            if (!isSearchOpen.value && isExpanded) {
                searchBarState.animateToCollapsed()
            }
        }

        val inputField = @Composable {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = {
                    query = it
                    searchTextChanged(it)
                },
                onSearch = {
                    searchTextChanged(it)
                    scope.launch { searchBarState.animateToCollapsed() }
                },
                expanded = isExpanded,
                onExpandedChange = { expanded ->
                    scope.launch {
                        if (expanded) searchBarState.animateToExpanded() else searchBarState.animateToCollapsed()
                    }
                },
                placeholder = { Text("Search messages and contacts...") },
                leadingIcon = {
                    if (isExpanded) {
                        IconButton(onClick = {
                            scope.launch { searchBarState.animateToCollapsed() }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            searchTextChanged("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SearchBar(
                state = searchBarState,
                inputField = inputField,
                modifier = Modifier.fillMaxWidth()
            )

            ExpandedFullScreenSearchBar(
                state = searchBarState,
                inputField = inputField,
            ) {
                if (query.length < 2) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getString(org.fossify.commons.R.string.type_2_characters),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (searchResultsStateList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getString(org.fossify.commons.R.string.no_items_found),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResultsStateList) { result ->
                            SearchResultItem(result = result) {
                                scope.launch { searchBarState.animateToCollapsed() }
                                Intent(context, ThreadActivity::class.java).apply {
                                    putExtra(THREAD_ID, result.threadId)
                                    putExtra(THREAD_TITLE, result.title)
                                    putExtra(SEARCHED_MESSAGE_ID, result.messageId)
                                    context.startActivity(this)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CategoryFilterRow(
        currentTab: Tab,
        selectedFilter: ConversationFilter,
        onFilterSelected: (ConversationFilter) -> Unit
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == ConversationFilter.ALL,
                    onClick = { onFilterSelected(ConversationFilter.ALL) },
                    label = { Text("All") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == ConversationFilter.UNREAD,
                    onClick = { onFilterSelected(ConversationFilter.UNREAD) },
                    label = { Text("Unread") }
                )
            }
            if (currentTab == Tab.PERSONAL) {
                item {
                    FilterChip(
                        selected = selectedFilter == ConversationFilter.FAVOURITES,
                        onClick = { onFilterSelected(ConversationFilter.FAVOURITES) },
                        label = { Text("Favourites") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == ConversationFilter.GROUP,
                        onClick = { onFilterSelected(ConversationFilter.GROUP) },
                        label = { Text("Groups") }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SelectionTopBar(
        selectedIds: List<Long>,
        onClose: () -> Unit,
        onDelete: () -> Unit,
        onArchive: () -> Unit
    ) {
        TopAppBar(
            title = { Text(text = "${selectedIds.size}") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close selection")
                }
            },
            actions = {
                IconButton(onClick = onArchive) {
                    Icon(painterResource(R.drawable.ic_archive_vector), contentDescription = "Archive")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }

    private fun askConfirmDelete(conversations: List<Conversation>, callback: () -> Unit) {
        val itemsCnt = conversations.size
        val items = resources.getQuantityString(R.plurals.delete_conversations, itemsCnt, itemsCnt)

        val baseString = org.fossify.commons.R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(this, question) {
            callback()
        }
    }

    private fun askConfirmArchive(conversations: List<Conversation>, callback: () -> Unit) {
        val itemsCnt = conversations.size
        val items = resources.getQuantityString(R.plurals.delete_conversations, itemsCnt, itemsCnt)

        val baseString = R.string.archive_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(this, question) {
            callback()
        }
    }

    private fun deleteThreads(threadIds: List<Long>) {
        ensureBackgroundThread {
            threadIds.forEach {
                deleteConversation(it)
                notificationManager.cancel(it.hashCode())
            }
            runOnUiThread {
                conversationsStateList.removeAll { threadIds.contains(it.threadId) }
                allLoadedConversations.removeAll { threadIds.contains(it.threadId) }
            }
        }
    }

    private fun archiveThreads(threadIds: List<Long>) {
        ensureBackgroundThread {
            threadIds.forEach {
                updateConversationArchivedStatus(it, true)
                notificationManager.cancel(it.hashCode())
            }
            runOnUiThread {
                conversationsStateList.removeAll { threadIds.contains(it.threadId) }
                allLoadedConversations.removeAll { threadIds.contains(it.threadId) }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ConversationListItem(
        conversation: Conversation,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        val isPinned = remember(conversation.threadId) {
            config.pinnedConversations.contains(conversation.threadId.toString())
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar image
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val timeStr = remember(conversation.date) {
                        (conversation.date * 1000L).formatDateOrTime(
                            context = this@MainActivity,
                            hideTimeOnOtherDays = true,
                            showCurrentYear = true
                        )
                    }

                    Text(
                        text = timeStr,
                        fontSize = 12.sp,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.snippet,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isPinned) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (conversation.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conversation.unreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchResultItem(
        result: SearchResult,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MailOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.date,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.snippet,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
