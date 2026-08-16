package com.smsg.data.repository
import android.content.Context
class EphemeralRepository(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("ephemeral", Context.MODE_PRIVATE)
    fun isPapaContact(address: String, name: String?): Boolean {
        return address.contains("papa", true) || (name?.contains("papa", true) == true)
    }
    fun isEphemeralEnabled(address: String): Boolean = prefs.getBoolean("eph_${address.lowercase()}", false)
    fun setEphemeralEnabled(address: String, enabled: Boolean) { prefs.edit().putBoolean("eph_${address.lowercase()}", enabled).apply() }
    fun getDurationMs(address: String): Long = prefs.getLong("eph_dur_${address.lowercase()}", 24*60*60*1000L) // 24h par défaut
    fun setDurationMs(address: String, ms: Long) { prefs.edit().putLong("eph_dur_${address.lowercase()}", ms).apply() }
}
