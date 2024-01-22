package com.example.greeing_card

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun Conversation(cas:List<GreetingCard>, navController: NavController, context: Context) {
    val (savedUsername, savedImageFilename) = getUserProfile(context)

    Column {
        Button(onClick = { navController.popBackStack() }) {
            Text("Go Back")
        }
        /*Button(onClick = { navController.navigate("settings") }) {
            Text("Settings")
        }
        Button(onClick = { navController.navigate("home") {
            popUpTo("home") { inclusive = false }
        }}) {
            Text("Home")
        }*/

        LazyColumn {
            items(cas) { ca ->
                Greeting(ca, savedUsername, savedImageFilename, context)
            }
        }
    }}