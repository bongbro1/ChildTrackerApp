package com.example.childtrackerapp.admin

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.childtrackerapp.R

import com.google.firebase.database.FirebaseDatabase
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lấy instance của Realtime Database
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("testConnection")

        // Thử ghi dữ liệu
        myRef.setValue("Hello Firebase!")
            .addOnSuccessListener {
                Log.d("FirebaseTest", "Kết nối Firebase thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTest", "Kết nối Firebase thất bại", e)
            }
    }
}
