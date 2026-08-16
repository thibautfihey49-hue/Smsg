package com.smsg.data.util
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
object NotifHelper {
    const val CH_ID = "sms_channel"
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH_ID, "Messages", NotificationManager.IMPORTANCE_HIGH)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
    fun showNewSms(ctx: Context, from: String, body: String) {
        ensureChannel(ctx)
        val notif = NotificationCompat.Builder(ctx, CH_ID).setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle(from).setContentText(body).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(ctx).notify(from.hashCode(), notif) } catch (e: SecurityException) {}
    }
}
