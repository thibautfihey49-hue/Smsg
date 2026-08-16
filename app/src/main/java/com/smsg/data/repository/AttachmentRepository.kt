package com.smsg.data.repository
import android.content.Context
import android.net.Uri
import java.io.File
class AttachmentRepository(private val ctx: Context) {
    fun copyToInternal(uri: Uri, prefix: String, ext: String): File {
        val input = ctx.contentResolver.openInputStream(uri)!!
        val file = File(ctx.filesDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        file.outputStream().use { input.copyTo(it) }
        return file
    }
    fun listForThread(threadId: Long): List<File> {
        return ctx.filesDir.listFiles { f -> f.name.contains(threadId.toString()) }?.toList() ?: emptyList()
    }
}
