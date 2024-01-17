package org.fossify.messages.extensions

import android.text.format.DateFormat
import java.util.Date

fun Date.format(pattern: String): String {
    return DateFormat.format(pattern, this).toString()
}
