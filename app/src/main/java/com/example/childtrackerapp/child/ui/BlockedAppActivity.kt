package com.example.childtrackerapp.child.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle

class BlockedAppActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra("appName") ?: "ứng dụng này"

        AlertDialog.Builder(this)
            .setTitle("Ứng dụng bị chặn")
            .setMessage("Mày đã bị ba mày cấm dùng $appName")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
