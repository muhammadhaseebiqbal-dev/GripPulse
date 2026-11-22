package com.example

import android.app.Application
import com.example.ai.GripAiAdvisor
import com.example.ai.PlaceholderGripAiAdvisor
import com.example.data.GripDatabase
import com.example.data.GripPreferencesRepository
import com.example.data.GripRepository
import com.example.sensor.GripSensorEngine

class GripPulseApp : Application() {

    lateinit var database: GripDatabase
        private set
    lateinit var preferences: GripPreferencesRepository
        private set
    lateinit var repository: GripRepository
        private set
    lateinit var sensorEngine: GripSensorEngine
        private set
    lateinit var aiAdvisor: GripAiAdvisor
        private set

    override fun onCreate() {
        super.onCreate()
        database = GripDatabase.getInstance(this)
        preferences = GripPreferencesRepository(this)
        repository = GripRepository(database.gripDao())
        sensorEngine = GripSensorEngine(
            context = this,
            repository = repository,
            preferences = preferences
        )
        aiAdvisor = PlaceholderGripAiAdvisor()
    }
}
