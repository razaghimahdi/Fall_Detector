package com.example.falldetector02.presentation.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.example.falldetector02.R
import com.example.falldetector02.presentation.util.other.ACTION_PAUSE_SERVICE
import com.example.falldetector02.presentation.util.other.ACTION_START_OR_RESUME_SERVICE
import com.example.falldetector02.presentation.util.other.ACTION_STOP_SERVICE
import com.example.falldetector02.presentation.services.FallDetectorService02


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FallDetectorService02.isDetecting.value?.let { isDetecting ->
            findViewById<SwitchCompat>(R.id.switch1)?.isChecked = isDetecting
        }

        findViewById<SwitchCompat>(R.id.switch1)?.apply {

            setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    start()
                } else {
                    start()
                }
            }


        }

    }

    private fun stop() {
        sendCommandToService(ACTION_STOP_SERVICE)
    }


    private fun start() {
        FallDetectorService02.isDetecting.value?.let { isDetecting ->
            if (isDetecting) {
                sendCommandToService(ACTION_PAUSE_SERVICE)
            } else {
                sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            }
        }
    }


    private fun sendCommandToService(action: String) =
        Intent(this, FallDetectorService02::class.java).also {
            it.action = action
            startService(it)
        }


}