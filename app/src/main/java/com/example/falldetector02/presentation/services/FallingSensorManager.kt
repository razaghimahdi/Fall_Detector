package com.example.falldetector02.presentation.services

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.util.Log
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

const val INTEGER_ZERO = 0
const val INTEGER_ONE = 1
const val DOUBLE_ZERO = 0.0

class FallingSensorManager {

    private val TAG = "AppDebug FallingSensorManager"

    @SuppressLint("LongLogTag")
    fun isFallHappened(event: SensorEvent?): Boolean {

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val loX: Double = event.values?.get( INTEGER_ZERO)?.toDouble() ?: DOUBLE_ZERO
            val loY: Double = event.values?.get( INTEGER_ONE)?.toDouble() ?: DOUBLE_ZERO
            val loZ: Double = event.values?.get(2)?.toDouble() ?:  DOUBLE_ZERO
            val loAccelerationReader = sqrt(
                loX.pow(2.0) + loY.pow(2.0) + loZ.pow(2.0)
            )
            val precision = DecimalFormat("0.00")
            val ldAccRound  = precision.format(loAccelerationReader).toDouble()
            Log.i(TAG, "isValidFall precision: "+precision)
            Log.i(TAG, "isValidFall ldAccRound: "+ldAccRound)
            if (ldAccRound > 0.3 && ldAccRound < 0.7) {
                Log.i(TAG, "isValidFall true: ")
                return true
            }
        }
        Log.i(TAG, "isValidFall false: ")
        return false
    }

}