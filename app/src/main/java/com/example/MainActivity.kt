package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.GripPulseRoot
import com.example.ui.GripPulseViewModel
import com.example.ui.theme.GripPulseTheme

class MainActivity : ComponentActivity() {

    private lateinit var app: GripPulseApp
    private lateinit var viewModel: GripPulseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        app = application as GripPulseApp
        viewModel = ViewModelProvider(
            this,
            GripPulseViewModel.factory(
                repository = app.repository,
                preferences = app.preferences,
                sensorEngine = app.sensorEngine,
                aiAdvisor = app.aiAdvisor
            )
        )[GripPulseViewModel::class.java]

        setContent {
            GripPulseTheme {
                GripPulseRoot(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.sensorEngine.acquireListener()
    }

    override fun onStop() {
        app.sensorEngine.releaseListener()
        super.onStop()
    }
}
