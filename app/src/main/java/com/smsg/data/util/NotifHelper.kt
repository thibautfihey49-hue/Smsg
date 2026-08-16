package com.smsg.data.util
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.smsg.MainActivity
import com.smsg.data.receiver.ReplyReceiver
object NotifHelper {
    const val CH_ID = "sms_channel"
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH_ID, "Messages", NotificationManager.IMPORTANCE_HIGH)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
    fun getContactName(ctx: Context, phone: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) { null }
    }
    fun showNewSms(ctx: Context, fromNumber: String, body: String) {
        ensureChannel(ctx)
        val name = getContactName(ctx, fromNumber)?: fromNumber
        val openIntent = Intent(ctx, MainActivity::class.java)
        val openPI = PendingIntent.getActivity(ctx, fromNumber.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val remoteInput = RemoteInput.Builder("key_text_reply").setLabel("Répondre à $name").build()
        val replyIntent = Intent(ctx, ReplyReceiver::class.java).apply { putExtra("address", fromNumber); putExtra("notif_id", fromNumber.hashCode()) }
        val replyPI = PendingIntent.getBroadcast(ctx, fromNumber.hashCode()+1, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val replyAction = NotificationCompat.Action.Builder(android.R.drawable.ic_dialog_email, "Répondre", replyPI).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build()
        val notif = NotificationCompat.Builder(ctx, CH_ID).setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle(name).setContentText(body)
           .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(openPI).addAction(replyAction)
           .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(ctx).notify(fromNumber.hashCode(), notif) } catch (e: Exception) {}
    }
}
