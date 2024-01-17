package org.fossify.messages.extensions

import android.text.TextUtils
import org.fossify.commons.models.SimpleContact

fun ArrayList<SimpleContact>.getThreadTitle(): String = TextUtils.join(", ", map { it.name }.toTypedArray()).orEmpty()

fun ArrayList<SimpleContact>.getAddresses() = flatMap { it.phoneNumbers }.map { it.normalizedNumber }
