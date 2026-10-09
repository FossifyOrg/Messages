package org.fossify.messages.messaging

import android.app.Activity
import android.content.Context
import android.telephony.SmsManager
import org.fossify.messages.R
import org.fossify.messages.receivers.SendStatusReceiver

@Suppress("CyclomaticComplexMethod")
fun Context.getSmsSendingError(
    resultCode: Int,
    errorCode: Int = SendStatusReceiver.NO_ERROR_CODE,
    noDefault: Boolean = false
): String? {
    if (resultCode == Activity.RESULT_OK) {
        return null
    }

    val messageId = if (noDefault) {
        R.string.sms_error_no_sim_selected
    } else {
        when (resultCode) {
            SmsManager.RESULT_ERROR_RADIO_OFF -> R.string.error_radio_turned_off
            SmsManager.RESULT_ERROR_NO_SERVICE,
            SmsManager.RESULT_RIL_NO_NETWORK_FOUND -> R.string.error_service_is_unavailable
            SmsManager.RESULT_RADIO_NOT_AVAILABLE,
            SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE -> R.string.sms_error_radio_unavailable

            SmsManager.RESULT_ERROR_NULL_PDU,
            SmsManager.RESULT_ENCODING_ERROR,
            SmsManager.RESULT_RIL_ENCODING_ERR -> R.string.sms_error_encoding

            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED,
            SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED -> R.string.sms_error_limit_exceeded

            SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> R.string.sms_error_fixed_dialing
            SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED,
            SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> R.string.sms_error_short_code_not_allowed

            SmsManager.RESULT_NETWORK_REJECT,
            SmsManager.RESULT_RIL_NETWORK_REJECT -> R.string.sms_error_network_rejected

            SmsManager.RESULT_INVALID_ARGUMENTS,
            SmsManager.RESULT_RIL_INVALID_ARGUMENTS -> R.string.sms_error_invalid_arguments

            SmsManager.RESULT_INVALID_STATE,
            SmsManager.RESULT_RIL_INVALID_STATE,
            SmsManager.RESULT_RIL_INVALID_MODEM_STATE -> R.string.sms_error_invalid_state

            SmsManager.RESULT_NO_MEMORY,
            SmsManager.RESULT_RIL_NO_MEMORY -> R.string.sms_error_no_memory

            SmsManager.RESULT_INVALID_SMS_FORMAT,
            SmsManager.RESULT_RIL_INVALID_SMS_FORMAT -> R.string.sms_error_invalid_format

            SmsManager.RESULT_SYSTEM_ERROR,
            SmsManager.RESULT_INTERNAL_ERROR,
            SmsManager.RESULT_REMOTE_EXCEPTION,
            SmsManager.RESULT_RIL_SYSTEM_ERR,
            SmsManager.RESULT_RIL_INTERNAL_ERR -> R.string.sms_error_system

            SmsManager.RESULT_MODEM_ERROR,
            SmsManager.RESULT_RIL_MODEM_ERR -> R.string.sms_error_modem

            SmsManager.RESULT_NETWORK_ERROR,
            SmsManager.RESULT_RIL_NETWORK_ERR -> R.string.sms_error_network

            SmsManager.RESULT_INVALID_SMSC_ADDRESS,
            SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS -> R.string.sms_error_service_center

            SmsManager.RESULT_OPERATION_NOT_ALLOWED,
            SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED -> R.string.sms_error_not_allowed

            SmsManager.RESULT_NO_RESOURCES,
            SmsManager.RESULT_RIL_NO_RESOURCES,
            SmsManager.RESULT_SMS_SEND_RETRY_FAILED,
            SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY -> R.string.message_send_retry_later

            SmsManager.RESULT_CANCELLED,
            SmsManager.RESULT_RIL_CANCELLED,
            SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING -> R.string.sms_error_cancelled

            SmsManager.RESULT_REQUEST_NOT_SUPPORTED,
            SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED -> R.string.sms_error_not_supported

            SmsManager.RESULT_NO_BLUETOOTH_SERVICE,
            SmsManager.RESULT_INVALID_BLUETOOTH_ADDRESS,
            SmsManager.RESULT_BLUETOOTH_DISCONNECTED -> R.string.sms_error_bluetooth

            SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY -> R.string.sms_error_emergency
            SmsManager.RESULT_NO_DEFAULT_SMS_APP -> R.string.sms_error_no_sim_selected
            SmsManager.RESULT_USER_NOT_ALLOWED -> R.string.sms_error_user_not_allowed
            SmsManager.RESULT_RIL_NETWORK_NOT_READY -> R.string.sms_error_network_not_ready
            SmsManager.RESULT_RIL_SIM_ABSENT -> R.string.sms_error_sim_missing
            SmsManager.RESULT_RIL_NO_SUBSCRIPTION,
            SmsManager.RESULT_RIL_SUBSCRIPTION_NOT_AVAILABLE -> R.string.message_send_sim_unavailable

            SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED,
            SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL -> R.string.sms_error_call_in_progress

            SmsManager.RESULT_RIL_ACCESS_BARRED -> R.string.sms_error_access_barred
            else -> R.string.message_not_sent_short
        }
    }
    val message = getString(messageId)
    return if (errorCode == SendStatusReceiver.NO_ERROR_CODE) {
        getString(R.string.sms_sending_error_details, message, resultCode)
    } else {
        getString(R.string.sms_sending_error_radio_details, message, resultCode, errorCode)
    }
}

@Suppress("CyclomaticComplexMethod")
fun Context.getMmsSendingError(resultCode: Int, httpStatus: Int = 0): String? {
    if (resultCode == Activity.RESULT_OK) {
        return null
    }

    val messageId = when (resultCode) {
        SmsManager.MMS_ERROR_INVALID_APN -> R.string.mms_error_invalid_apn
        SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS -> R.string.mms_error_connection
        SmsManager.MMS_ERROR_HTTP_FAILURE -> R.string.mms_error_http
        SmsManager.MMS_ERROR_IO_ERROR -> R.string.mms_error_io
        SmsManager.MMS_ERROR_RETRY -> R.string.message_send_retry_later
        SmsManager.MMS_ERROR_CONFIGURATION_ERROR -> R.string.mms_error_configuration
        SmsManager.MMS_ERROR_NO_DATA_NETWORK -> R.string.mms_error_no_network
        SmsManager.MMS_ERROR_INVALID_SUBSCRIPTION_ID -> R.string.message_send_sim_unavailable
        SmsManager.RESULT_NO_DEFAULT_SMS_APP -> R.string.sms_error_no_sim_selected
        SmsManager.MMS_ERROR_INACTIVE_SUBSCRIPTION -> R.string.mms_error_sim_inactive
        SmsManager.MMS_ERROR_DATA_DISABLED -> R.string.mms_error_data_disabled
        SmsManager.MMS_ERROR_MMS_DISABLED_BY_CARRIER -> R.string.mms_error_carrier_disabled
        else -> R.string.message_not_sent_short
    }
    val message = getString(messageId)
    return if (httpStatus == 0) {
        getString(R.string.mms_sending_error_details, message, resultCode)
    } else {
        getString(R.string.mms_sending_error_http_details, message, resultCode, httpStatus)
    }
}
