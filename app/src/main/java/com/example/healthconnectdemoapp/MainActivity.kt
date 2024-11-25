package com.example.healthconnectdemoapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

class MainActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    private val requiredPermissions = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                readStepsData()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_read_steps).setOnClickListener {
            readStepsData()
        }

        findViewById<Button>(R.id.btn_write_steps).setOnClickListener {
            writeStepsData()
        }
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        requestPermissions()
    }

    private fun requestPermissions() {
        lifecycleScope.launch {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

            val permissionsToRequest = requiredPermissions.filterNot { grantedPermissions.contains(it) }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                readStepsData()
            }
        }
    }

    private fun readStepsData() {
        lifecycleScope.launch {
            try {
                val now = Instant.now()
                val startOfDay = now.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()

                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )

                val response = healthConnectClient.readRecords(request)

                Log.i("MYTAG", "size=${response.records.size}")

                response.records.forEach { record ->
                    val steps = record.count
                    val start = record.startTime
                    val end = record.endTime
                    Log.i("MYTAG", "Steps: $steps, Start: $start, End: $end")
                }
            } catch (e: Exception) {
                Log.i("MYTAG", "Exception: $e")
                e.printStackTrace()
            }
        }
    }

    private fun writeStepsData() {
        lifecycleScope.launch {
            try {
                val now = Instant.now()
                val oneHourAgo = now.minusSeconds(3600)

                val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())


                val stepsRecord = StepsRecord(
                    count = 5000,
                    startTime = oneHourAgo,
                    endTime = now,
                    startZoneOffset = zoneOffset,
                    endZoneOffset = zoneOffset
                )

                healthConnectClient.insertRecords(listOf(stepsRecord))
                Log.i("MYTAG", "Steps data written successfully.")
            } catch (e: Exception) {
                Log.i("MYTAG", "Exception: $e")
                e.printStackTrace()
            }
        }
    }
}

