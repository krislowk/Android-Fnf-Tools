package com.zen.fnftools.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast

object SaveAccess {

    fun isGranted(): Boolean = Environment.isExternalStorageManager()

    /** Sends the user to the system settings screen to grant All Files Access. */
    fun requestAccess(activity: Activity) {
        Toast.makeText(
            activity,
            "Grant \"Allow access to manage all files\" so Zen FNF Toolkit can save to /ZFNFTSaves, then come back and tap Pack & Save again",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }
}
