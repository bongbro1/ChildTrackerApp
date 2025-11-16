package com.example.childtrackerapp.child.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.example.childtrackerapp.Athu.viewmodel.AuthViewModel

import com.example.childtrackerapp.child.ui.screen.ChildMainScreen
import com.example.childtrackerapp.child.viewmodel.ChildViewModel
import com.example.childtrackerapp.parent.ui.view.ParentMainScreen
import com.example.childtrackerapp.ui.theme.ChildTrackerTheme

import kotlin.getValue


class MainActivity_Child : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val childViewModel: ChildViewModel by viewModels()
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                ChildTrackerTheme {
                    ChildMainScreen(authViewModel, childViewModel)
                }
            }
        }
    }
