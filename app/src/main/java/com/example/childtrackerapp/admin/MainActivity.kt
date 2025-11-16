package com.example.childtrackerapp.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


import com.example.childtrackerapp.child.ui.MainActivity_Child

import com.example.childtrackerapp.databinding.ActivityMainBinding
import com.example.childtrackerapp.parent.ui.view.ParentMainActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()



    }
}
