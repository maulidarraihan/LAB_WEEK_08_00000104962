package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        notificationBuilder = startForegroundService()

        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundService(): NotificationCompat.Builder {

        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val builder = getNotificationBuilder(pendingIntent, channelId)

        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), flag
        )
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = "001"
            val name = "001 Channel"
            val priority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(id, name, priority)
            val manager =
                ContextCompat.getSystemService(this, NotificationManager::class.java)!!
            manager.createNotificationChannel(channel)
            return id
        }
        return ""
    }

    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setTicker("Second worker done!")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val returnValue = super.onStartCommand(intent, flags, startId)

        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            countDown(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return returnValue
    }

    private fun countDown(builder: NotificationCompat.Builder) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            builder.setContentText("$i seconds remaining")
            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
