package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.rescheduleAllScheduledMessages

/**
 * Reschedules exact alarms after boot/package updates and catches up overdue scheduled messages.
 */
class RescheduleAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        ensureBackgroundThread {
            context.rescheduleAllScheduledMessages()
            pendingResult.finish()
        }
    }
}
