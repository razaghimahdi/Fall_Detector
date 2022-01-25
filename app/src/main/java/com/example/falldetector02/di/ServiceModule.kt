package com.example.falldetector02.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.falldetector02.R
import com.example.falldetector02.presentation.util.other.ACTION_SHOW_DETECTOR
import com.example.falldetector02.presentation.util.other.NOTIFICATION_CHANNEL_ID
import com.example.falldetector02.presentation.services.FallingSensorManager
import com.example.falldetector02.presentation.ui.MainActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Qualifier
import kotlin.math.roundToInt

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun provideFallingSensorManager() = FallingSensorManager()

    @ServiceScoped
    @Provides
    fun provideMainActivityPendingIntent(
        @ApplicationContext app: Context
    ) = PendingIntent.getActivity(
        app,
        0,
        Intent(app, MainActivity::class.java).also {
            it.action = ACTION_SHOW_DETECTOR
        },
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    @ServiceScoped
    @Provides
    @Impl1
    fun provideBaseDetectionNotificationBuilder(
        @ApplicationContext app: Context,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(app, "${NOTIFICATION_CHANNEL_ID}.new_detection")
            .setAutoCancel(true)
            .setOngoing(false)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Fall detected")
            .setContentText("fall detection duration")
    }


    @ServiceScoped
    @Provides
    @Impl2
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context,
        pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Fall Detector App")
        .setContentText("Fall Detector is running...")
        .setContentIntent(pendingIntent)


    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Impl1

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Impl2


}