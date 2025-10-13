package org.fossify.messages

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import org.fossify.commons.FossifyApp
import org.fossify.messages.helpers.MessagingCache

class App : FossifyApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        listOf(
            ContactsContract.Contacts.CONTENT_URI,
            ContactsContract.Data.CONTENT_URI,
            ContactsContract.DisplayPhoto.CONTENT_URI
        ).forEach {
            contentResolver.registerContentObserver(it, true, contactsObserver)
        }
    }

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            MessagingCache.namePhoto.evictAll()
        }
    }
}
