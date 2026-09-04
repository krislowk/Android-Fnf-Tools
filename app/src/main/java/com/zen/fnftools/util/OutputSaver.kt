package com.zen.fnftools.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/**
 * Saves outputs directly into /storage/emulated/0/ZFNFTSaves — a single flat
 * folder for both the PNG and its XML, rather than splitting across the
 * Pictures/Downloads MediaStore collections. Requires "All files access"
 * (MANAGE_EXTERNAL_STORAGE), which SaveAccess.ensureGranted() handles.
 */
object OutputSaver {

    private const val FOLDER_NAME = "ZFNFTSaves"

    fun saveDir(): File {
        val dir = File(android.os.Environment.getExternalStorageDirectory(), FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun savePng(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val file = File(saveDir(), fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveXml(context: Context, xmlContent: String, fileName: String): Boolean {
        return try {
            val file = File(saveDir(), fileName)
            file.writeText(xmlContent, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            false
        }
    }
}

