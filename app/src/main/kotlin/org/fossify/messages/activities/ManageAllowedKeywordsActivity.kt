package org.fossify.messages.activities

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getTempFile
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.underlineText
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ExportResult
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivityManageAllowedKeywordsBinding
import org.fossify.messages.dialogs.AddAllowedKeywordDialog
import org.fossify.messages.dialogs.ExportAllowedKeywordsDialog
import org.fossify.messages.dialogs.ManageAllowedKeywordsAdapter
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.toArrayList
import org.fossify.messages.helpers.AllowedKeywordsExporter
import org.fossify.messages.helpers.AllowedKeywordsImporter
import java.io.FileOutputStream
import java.io.OutputStream

class ManageAllowedKeywordsActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityManageAllowedKeywordsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateAllowedKeywords()
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.manageAllowedKeywordsList))
        setupMaterialScrollListener(
            scrollingView = binding.manageAllowedKeywordsList,
            topAppBar = binding.allowKeywordsAppbar
        )
        updateTextColors(binding.manageAllowedKeywordsWrapper)

        binding.manageAllowedKeywordsPlaceholder2.apply {
            underlineText()
            setTextColor(getProperPrimaryColor())
            setOnClickListener {
                addOrEditAllowedKeyword()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.allowKeywordsAppbar, NavigationIcon.Arrow)
    }

    private fun setupOptionsMenu() {
        binding.allowKeywordsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_allowed_keyword -> {
                    addOrEditAllowedKeyword()
                    true
                }

                R.id.export_allowed_keywords -> {
                    tryExportAllowedKeywords()
                    true
                }

                R.id.import_allowed_keywords -> {
                    tryImportAllowedKeywords()
                    true
                }

                else -> false
            }
        }
    }

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            try {
                val outputStream = uri?.let { contentResolver.openOutputStream(it) }
                if (outputStream != null) {
                    exportAllowedKeywordsTo(outputStream)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            try {
                if (uri != null) {
                    tryImportAllowedKeywordsFromFile(uri)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

    private fun tryImportAllowedKeywords() {
        val mimeType = "text/plain"
        try {
            getContent.launch(mimeType)
        } catch (_: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun tryImportAllowedKeywordsFromFile(uri: Uri) {
        when (uri.scheme) {
            "file" -> importAllowedKeywords(uri.path!!)
            "content" -> {
                val tempFile = getTempFile("allowed", "allowed_keywords.txt")
                if (tempFile == null) {
                    toast(org.fossify.commons.R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    importAllowedKeywords(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }

            else -> toast(org.fossify.commons.R.string.invalid_file_format)
        }
    }

    private fun importAllowedKeywords(path: String) {
        ensureBackgroundThread {
            val result = AllowedKeywordsImporter(this).importAllowedKeywords(path)
            toast(
                when (result) {
                    AllowedKeywordsImporter.ImportResult.IMPORT_OK -> org.fossify.commons.R.string.importing_successful
                    AllowedKeywordsImporter.ImportResult.IMPORT_FAIL -> org.fossify.commons.R.string.no_items_found
                }
            )
            updateAllowedKeywords()
        }
    }

    private fun exportAllowedKeywordsTo(outputStream: OutputStream?) {
        ensureBackgroundThread {
            val allowedKeywords = config.allowedKeywords.toArrayList()
            if (allowedKeywords.isEmpty()) {
                toast(org.fossify.commons.R.string.no_entries_for_exporting)
            } else {
                AllowedKeywordsExporter.exportAllowedKeywords(allowedKeywords, outputStream) {
                    toast(
                        when (it) {
                            ExportResult.EXPORT_OK -> org.fossify.commons.R.string.exporting_successful
                            else -> org.fossify.commons.R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    private fun tryExportAllowedKeywords() {
        ExportAllowedKeywordsDialog(
            activity = this,
            path = config.lastAllowedKeywordExportPath,
            hidePath = true
        ) { file ->
            try {
                createDocument.launch(file.name)
            } catch (_: ActivityNotFoundException) {
                toast(
                    org.fossify.commons.R.string.system_service_disabled,
                    Toast.LENGTH_LONG
                )
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    override fun refreshItems() {
        updateAllowedKeywords()
    }

    private fun updateAllowedKeywords() {
        ensureBackgroundThread {
            val allowedKeywords = config.allowedKeywords.sorted().toArrayList()
            runOnUiThread {
                ManageAllowedKeywordsAdapter(
                    activity = this,
                    allowedKeywords = allowedKeywords,
                    listener = this,
                    recyclerView = binding.manageAllowedKeywordsList
                ) {
                    addOrEditAllowedKeyword(it as String)
                }.apply {
                    binding.manageAllowedKeywordsList.adapter = this
                }

                binding.manageAllowedKeywordsPlaceholder.beVisibleIf(allowedKeywords.isEmpty())
                binding.manageAllowedKeywordsPlaceholder2.beVisibleIf(allowedKeywords.isEmpty())
            }
        }
    }

    private fun addOrEditAllowedKeyword(keyword: String? = null) {
        AddAllowedKeywordDialog(this, keyword) {
            updateAllowedKeywords()
        }
    }
}
