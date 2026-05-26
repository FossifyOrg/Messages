package org.fossify.messages.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fossify.commons.activities.ManageBlockedNumbersActivity
import org.fossify.commons.dialogs.*
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.RadioItem
import org.fossify.messages.R
import org.fossify.messages.dialogs.ExportMessagesDialog
import org.fossify.messages.extensions.*
import org.fossify.messages.helpers.*
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private var blockedNumbersAtPause = -1
    private var recycleBinMessages = mutableStateOf(0)
    private val messagesFileType = "application/json"
    private val messageImportFileTypes = buildList {
        add("application/json")
        add("application/xml")
        add("text/xml")
        if (!isQPlus()) {
            add("application/octet-stream")
        }
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                MessagesImporter(this).importMessages(uri)
            }
        }

    private var exportMessagesDialog: ExportMessagesDialog? = null

    private val saveDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument(messagesFileType)) { uri ->
            if (uri != null) {
                toast(org.fossify.commons.R.string.exporting)
                exportMessagesDialog?.exportMessages(uri)
            }
        }

    // Compose state preference bindings
    private var useEnglishState = mutableStateOf(false)
    private var fontSizeState = mutableStateOf(FONT_SIZE_MEDIUM)
    private var showCharCounterState = mutableStateOf(false)
    private var useSimpleCharsState = mutableStateOf(false)
    private var sendOnEnterState = mutableStateOf(false)
    private var deliveryReportsState = mutableStateOf(false)
    private var sendLongMmsState = mutableStateOf(false)
    private var sendGroupMmsState = mutableStateOf(false)
    private var keepArchivedState = mutableStateOf(false)
    private var mmsFileSizeState = mutableLongStateOf(FILE_SIZE_NONE)
    private var useRecycleBinState = mutableStateOf(false)
    private var appPasswordState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read preferences
        useEnglishState.value = config.useEnglish
        fontSizeState.value = config.fontSize
        showCharCounterState.value = config.showCharacterCounter
        useSimpleCharsState.value = config.useSimpleCharacters
        sendOnEnterState.value = config.sendOnEnter
        deliveryReportsState.value = config.enableDeliveryReports
        sendLongMmsState.value = config.sendLongMessageMMS
        sendGroupMmsState.value = config.sendGroupMessageMMS
        keepArchivedState.value = config.keepConversationsArchived
        mmsFileSizeState.longValue = config.mmsFileSizeLimit
        useRecycleBinState.value = config.useRecycleBin
        appPasswordState.value = config.isAppPasswordProtectionOn

        setContent {
            MessagesTheme {
                SettingsScreen()
            }
        }

        loadRecycleBinSize()
    }

    override fun onResume() {
        super.onResume()
        if (blockedNumbersAtPause != -1 && blockedNumbersAtPause != getBlockedNumbers().hashCode()) {
            refreshConversations()
        }
    }

    override fun onPause() {
        super.onPause()
        blockedNumbersAtPause = getBlockedNumbers().hashCode()
    }

    private fun loadRecycleBinSize() {
        ensureBackgroundThread {
            val count = messagesDB.getArchivedCount()
            runOnUiThread {
                recycleBinMessages.value = count
            }
        }
    }

    private fun triggerExport() {
        exportMessagesDialog = ExportMessagesDialog(this) { fileName ->
            saveDocument.launch("$fileName.json")
        }
    }

    private fun triggerImport() {
        getContent.launch(messageImportFileTypes.toTypedArray())
    }

    private fun launchFontSizeDialog() {
        val items = arrayListOf(
            RadioItem(FONT_SIZE_SMALL, getString(org.fossify.commons.R.string.small)),
            RadioItem(FONT_SIZE_MEDIUM, getString(org.fossify.commons.R.string.medium)),
            RadioItem(FONT_SIZE_LARGE, getString(org.fossify.commons.R.string.large)),
            RadioItem(FONT_SIZE_EXTRA_LARGE, getString(org.fossify.commons.R.string.extra_large))
        )
        RadioGroupDialog(this, items, config.fontSize) {
            val pickedSize = it as Int
            config.fontSize = pickedSize
            fontSizeState.value = pickedSize
        }
    }

    private fun triggerEmptyRecycleBin() {
        if (recycleBinMessages.value == 0) {
            toast(org.fossify.commons.R.string.recycle_bin_empty)
        } else {
            ConfirmationDialog(
                activity = this,
                message = "",
                messageId = R.string.empty_recycle_bin_messages_confirmation,
                positive = org.fossify.commons.R.string.yes,
                negative = org.fossify.commons.R.string.no
            ) {
                ensureBackgroundThread {
                    emptyMessagesRecycleBin()
                    runOnUiThread {
                        recycleBinMessages.value = 0
                    }
                }
            }
        }
    }

    private fun triggerPasswordProtection() {
        val tabToShow = if (config.isAppPasswordProtectionOn) {
            config.appProtectionType
        } else {
            SHOW_ALL_TABS
        }

        SecurityDialog(
            activity = this,
            requiredHash = config.appPasswordHash,
            showTabIndex = tabToShow
        ) { hash, type, success ->
            if (success) {
                val currentlyOn = config.isAppPasswordProtectionOn
                config.isAppPasswordProtectionOn = !currentlyOn
                config.appPasswordHash = if (currentlyOn) "" else hash
                config.appProtectionType = type
                appPasswordState.value = !currentlyOn

                if (config.isAppPasswordProtectionOn) {
                    val confirmationTextId = if (config.appProtectionType == PROTECTION_FINGERPRINT) {
                        org.fossify.commons.R.string.fingerprint_setup_successfully
                    } else {
                        org.fossify.commons.R.string.protection_setup_successfully
                    }
                    ConfirmationDialog(
                        activity = this,
                        message = "",
                        messageId = confirmationTextId,
                        positive = org.fossify.commons.R.string.ok,
                        negative = 0
                    ) {}
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Compose Settings Interface Implementation (Jetpack Compose M3)
    // -------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getString(org.fossify.commons.R.string.settings), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Appearance
                item {
                    SettingsCard(title = getString(org.fossify.commons.R.string.color_customization)) {
                        SettingsItem(
                            title = "Theme Color Settings",
                            subtitle = "Adjust system manual primary, background, and text overrides",
                            onClick = { startCustomizationActivity() }
                        )
                        SettingsItem(
                            title = getString(org.fossify.commons.R.string.font_size),
                            subtitle = getFontSizeLabel(fontSizeState.value),
                            onClick = { launchFontSizeDialog() }
                        )
                        SettingsItem(
                            title = "Language",
                            subtitle = Locale.getDefault().displayLanguage,
                            onClick = {
                                if (isTiramisuPlus()) {
                                    launchChangeAppLanguageIntent()
                                }
                            }
                        )
                    }
                }

                // Section 2: Outgoing Messaging
                item {
                    SettingsCard(title = getString(R.string.outgoing_messages)) {
                        SettingsSwitchItem(
                            title = getString(R.string.show_character_counter),
                            subtitle = "Displays SMS length counters as you write messages",
                            checked = showCharCounterState.value,
                            onCheckedChange = {
                                config.showCharacterCounter = it
                                showCharCounterState.value = it
                            }
                        )
                        SettingsSwitchItem(
                            title = getString(R.string.use_simple_characters),
                            subtitle = "Converts special characters to standard accents",
                            checked = useSimpleCharsState.value,
                            onCheckedChange = {
                                config.useSimpleCharacters = it
                                useSimpleCharsState.value = it
                            }
                        )
                        SettingsSwitchItem(
                            title = getString(R.string.send_on_enter),
                            subtitle = "Pressing enter key sends the current message block",
                            checked = sendOnEnterState.value,
                            onCheckedChange = {
                                config.sendOnEnter = it
                                sendOnEnterState.value = it
                            }
                        )
                        SettingsSwitchItem(
                            title = getString(R.string.enable_delivery_reports),
                            subtitle = "Request SMS delivery checklists from carrier",
                            checked = deliveryReportsState.value,
                            onCheckedChange = {
                                config.enableDeliveryReports = it
                                deliveryReportsState.value = it
                            }
                        )
                        SettingsSwitchItem(
                            title = getString(R.string.send_long_message_mms),
                            subtitle = "Coverts long multi-page text blocks into MMS transmissions",
                            checked = sendLongMmsState.value,
                            onCheckedChange = {
                                config.sendLongMessageMMS = it
                                sendLongMmsState.value = it
                            }
                        )
                        SettingsSwitchItem(
                            title = getString(R.string.group_message_mms),
                            subtitle = "Encodes group recipients messages in standard MMS layout",
                            checked = sendGroupMmsState.value,
                            onCheckedChange = {
                                config.sendGroupMessageMMS = it
                                sendGroupMmsState.value = it
                            }
                        )
                    }
                }

                // Section 3: Data Migration & Security
                item {
                    SettingsCard(title = "Migration & Security") {
                        SettingsSwitchItem(
                            title = getString(org.fossify.commons.R.string.password_protect_whole_app),
                            subtitle = "Require biometric or passcode triggers to open app",
                            checked = appPasswordState.value,
                            onCheckedChange = { triggerPasswordProtection() }
                        )
                        SettingsItem(
                            title = getString(R.string.export_messages),
                            subtitle = "Back up text history records locally as JSON metadata",
                            onClick = { triggerExport() }
                        )
                        SettingsItem(
                            title = getString(R.string.import_messages),
                            subtitle = "Restore database items from previously saved backups",
                            onClick = { triggerImport() }
                        )
                    }
                }

                // Section 4: Chat Hub Actions
                item {
                    SettingsCard(title = "Conversation Actions") {
                        SettingsItem(
                            title = getString(org.fossify.commons.R.string.manage_blocked_numbers),
                            subtitle = "Configure call or contact blacklists directly",
                            onClick = {
                                if (isOrWasThankYouInstalled()) {
                                    startActivity(Intent(this@SettingsActivity, ManageBlockedNumbersActivity::class.java))
                                } else {
                                    FeatureLockedDialog(this@SettingsActivity) {}
                                }
                            }
                        )
                        SettingsItem(
                            title = getString(R.string.blocked_keywords),
                            subtitle = "Auto-archive incoming messages containing designated keywords",
                            onClick = {
                                if (isOrWasThankYouInstalled()) {
                                    startActivity(Intent(this@SettingsActivity, ManageBlockedKeywordsActivity::class.java))
                                } else {
                                    FeatureLockedDialog(this@SettingsActivity) {}
                                }
                            }
                        )
                        SettingsSwitchItem(
                            title = getString(R.string.keep_conversations_archived),
                            subtitle = "Prevent archived threads from waking up on incoming texts",
                            checked = keepArchivedState.value,
                            onCheckedChange = {
                                config.keepConversationsArchived = it
                                keepArchivedState.value = it
                            }
                        )
                        SettingsSwitchItem(
                            title = getString(org.fossify.commons.R.string.move_items_into_recycle_bin),
                            subtitle = "Deleted conversations are held temporarily before total purge",
                            checked = useRecycleBinState.value,
                            onCheckedChange = {
                                config.useRecycleBin = it
                                useRecycleBinState.value = it
                            }
                        )
                        if (useRecycleBinState.value) {
                            SettingsItem(
                                title = getString(org.fossify.commons.R.string.empty_recycle_bin),
                                subtitle = "Count: ${recycleBinMessages.value} items currently held",
                                onClick = { triggerEmptyRecycleBin() }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsCard(
        title: String,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }

    @Composable
    private fun SettingsItem(
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun SettingsSwitchItem(
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    private fun getFontSizeLabel(size: Int) = when (size) {
        FONT_SIZE_SMALL -> getString(org.fossify.commons.R.string.small)
        FONT_SIZE_MEDIUM -> getString(org.fossify.commons.R.string.medium)
        FONT_SIZE_LARGE -> getString(org.fossify.commons.R.string.large)
        else -> getString(org.fossify.commons.R.string.extra_large)
    }
}
