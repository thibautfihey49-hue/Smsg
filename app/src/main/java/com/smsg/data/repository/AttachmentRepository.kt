package com.smsg.data.repository
import android.content.Context
import android.net.Uri
import java.io.File
class AttachmentRepository(private val ctx: Context) {
    fun copyToInternal(uri: Uri, prefix: String, ext: String): File {
        val input = ctx.contentResolver.openInputStream(uri)?: throw Exception("no stream")
        val file = File(ctx.filesDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        file.outputStream().use { input.copyTo(it) }
        if (file.length()<100) throw Exception("fichier vide")
        return file
    }
}
