package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy { WorkManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // (1) Request permission notif (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        // (2) Constraints (internet only)
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        // (3) FIRST WORKER
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // (4) SECOND WORKER
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // (5) THIRD WORKER
        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()


        // (6) SET ORDER:
        // First → Second → NotificationService → Third → SecondNotificationService
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()


        // (7) Observer FIRST
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        // (8) Observer SECOND
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Second process is done")
                    launchNotificationService()
                }
            }

        // (9) Observer THIRD
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Third process is done")
                    launchSecondNotificationService()
                }
            }
    }

    // PACK INPUT
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    // SHOW TOAST
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    // (10) Start NotificationService
    private fun launchNotificationService() {

        NotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }

        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // (11) Start SecondNotificationService
    private fun launchSecondNotificationService() {

        SecondNotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(EXTRA_ID, "002")
        }

        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
