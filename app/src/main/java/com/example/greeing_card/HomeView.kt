package com.example.greeing_card

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import coil.compose.rememberAsyncImagePainter
import kotlin.math.abs



fun saveImageToInternalStorage(context: Context, uri: Uri): String {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

    val filename = "profile_image_${System.currentTimeMillis()}.jpg"
    context.openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
    }
    return filename
}



fun saveUserProfile(context: Context, username: String, imageFilename: String) {
    val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    sharedPrefs.edit().apply {
        putString("username", username)
        putString("imageFilename", imageFilename)
        apply()
    }
}

fun getUserProfile(context: Context): Pair<String?, String?> {
    val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    val username = sharedPrefs.getString("username", null)
    val imageFilename = sharedPrefs.getString("imageFilename", null)
    return Pair(username, imageFilename)
}
fun loadImageFromInternalStorage(context: Context, filename: String): Bitmap? {
    return try {
        val fis = context.openFileInput(filename)
        BitmapFactory.decodeStream(fis)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}




@Composable
fun HomeView(navController: NavController, context: Context) {
    var xChange by remember { mutableStateOf(0f) }
    var yChange by remember { mutableStateOf(0f) }
    var zChange by remember { mutableStateOf(0f) }
    var isNotificationEnabled by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var bitmapImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(key1 = true) {
        val (savedUsername, savedImageFilename) = getUserProfile(context)
        username = savedUsername ?: ""
        savedImageFilename?.let { filename ->
            val bitmap = loadImageFromInternalStorage(context, filename)
            bitmapImage = bitmap?.asImageBitmap()
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val filename = saveImageToInternalStorage(context, uri) // Save image and get filename
            saveUserProfile(context, username, filename) // Save filename
            val bitmap = loadImageFromInternalStorage(context, filename)
            bitmapImage = bitmap?.asImageBitmap() // Update the bitmapImage for display
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }



    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { pickMedia.launch("image/*") }) {
            Text("Select Profile Picture")
        }
        Spacer(modifier = Modifier.height(8.dp))
        bitmapImage?.let { imageBitmap ->
            Image(
                bitmap = imageBitmap,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        /*Button(onClick = {
            saveUserProfile(context, username, bitmapImage.toString())
        }) {
            Text("Save Profile")
        }*/
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("conversation"){

            popUpTo("home") { inclusive = false }
        } }) {
            Text("Go to Conversation")
        }
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        val permissionResult = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {

                val notification = createNotification(context)
                notificationManagerCompat.notify(Constants.NOTIFICATION_ID, notification)
            } else {

            }
        }



        Button(onClick = {
            isNotificationEnabled = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                } else {
                    permissionResult.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }) {
            Text("Enable Notifications")
        }

        var isSpinning = false
        //var xTotalChange = 0f
        //var yTotalChange = 0f
        //var zTotalChange = 0f
        val SPINNING_THRESHOLD = 1
        val STOP_SPINNING_THRESHOLD = 0.2
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        val sensorEventListener = object : SensorEventListener {

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

            }

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

                    xChange = event.values[0]
                    yChange = event.values[1]
                    zChange = event.values[2]

                    if (abs(xChange) > SPINNING_THRESHOLD || abs(yChange) > SPINNING_THRESHOLD || abs(zChange) > SPINNING_THRESHOLD) {
                        isSpinning = true
                        //xTotalChange += xChange
                        //yTotalChange += yChange
                        //zTotalChange += zChange
                    } else if (isSpinning && abs(xChange) < STOP_SPINNING_THRESHOLD && abs(yChange) < STOP_SPINNING_THRESHOLD && abs(zChange) < STOP_SPINNING_THRESHOLD) {

                        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

                                val notification = createNotificationWithSensorData(context, xTotalChange, yTotalChange, zTotalChange)
                                notificationManagerCompat.notify(Constants.NOTIFICATION_ID, notification)
                            } else {

                            }
                        } else {

                            val notification = createNotificationWithSensorData(context, xTotalChange, yTotalChange, zTotalChange)
                            notificationManagerCompat.notify(Constants.NOTIFICATION_ID, notification)
                        }*/

                        //xTotalChange = 0f
                        //yTotalChange = 0f
                        //zTotalChange = 0f
                        isSpinning = false
                    }
                }
            }
        }
        DisposableEffect(Unit) {
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)

            onDispose {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }
        Text("X-axis change: $xChange")
        Text("Y-axis change: $yChange")
        Text("Z-axis change: $zChange")





        /*Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("settings") }) {
            Text("Settings")
        }*/
    }





}