package org.fossify.messages.messaging

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import org.fossify.messages.helpers.SCHEDULED_MESSAGE_ID
import org.fossify.messages.helpers.THREAD_ID
import org.fossify.messages.models.Message
import org.fossify.messages.receivers.ScheduledMessageReceiver

/**
 * All things related to scheduled messages are here.
 */

fun Context.getScheduleSendPendingIntent(message: Message): PendingIntent {
    val intent = Intent(this, ScheduledMessageReceiver::class.java)
    intent.putExtra(THREAD_ID, message.threadId)
    intent.putExtra(SCHEDULED_MESSAGE_ID, message.id)

    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(this, message.id.toInt(), intent, flags)
}

fun Context.scheduleMessage(message: Message) {
    val pendingIntent = getScheduleSendPendingIntent(message)
    val triggerAtMillis = message.millis()

    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
}

fun Context.cancelScheduleSendPendingIntent(messageId: Long) {
    val intent = Intent(this, ScheduledMessageReceiver::class.java)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    PendingIntent.getBroadcast(this, messageId.toInt(), intent, flags).cancel()
}
