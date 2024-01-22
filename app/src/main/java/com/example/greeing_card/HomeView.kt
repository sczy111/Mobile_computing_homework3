package com.example.greeing_card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

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
        /*Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("settings") }) {
            Text("Settings")
        }*/
    }





}