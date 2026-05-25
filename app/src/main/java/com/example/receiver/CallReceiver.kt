package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.example.data.AppDatabase
import com.example.service.CallAlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (incomingNumber != null) {
                    checkAndTriggerAlert(context, incomingNumber)
                } else {
                    // Fallback to latest log entry or just continuous scan (some Android builds might delay number delivery)
                    // Let's run a small database comparison when number is retrieved
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK, TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call answered or finished, immediately stop alert rings
                stopAlert(context)
            }
        }
    }

    private fun checkAndTriggerAlert(context: Context, incomingNumber: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val enabledContacts = db.contactDao().getEnabledContactsList()
                
                var matchedContactName = ""
                var isMatched = false

                for (contact in enabledContacts) {
                    // Normalize comparison using Android standard comparison
                    if (PhoneNumberUtils.compare(context, contact.phoneNumber, incomingNumber) || 
                        PhoneNumberUtils.compare(context, contact.normalizedNumber, incomingNumber)) {
                        isMatched = true
                        matchedContactName = contact.name
                        break
                    }
                }

                if (isMatched) {
                    // Trigger loud alert service
                    try {
                        val serviceIntent = Intent(context, CallAlertService::class.java).apply {
                            putExtra(CallAlertService.EXTRA_CONTACT_NAME, matchedContactName)
                            putExtra(CallAlertService.EXTRA_CONTACT_NUMBER, incomingNumber)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun stopAlert(context: Context) {
        try {
            val serviceIntent = Intent(context, CallAlertService::class.java)
            context.stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
