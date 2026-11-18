package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PersonRepository
import com.example.ui.LockerViewModel
import com.example.ui.screens.DirectoryScreen
import com.example.ui.theme.SzekrenyTarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val database = AppDatabase.getInstance(applicationContext)
        val repository = PersonRepository(database.personDao())

        setContent {
            SzekrenyTarTheme {
                val lockerViewModel: LockerViewModel = viewModel(
                    factory = LockerViewModel.factory(repository),
                )
                DirectoryScreen(viewModel = lockerViewModel)
            }
        }
    }
}
