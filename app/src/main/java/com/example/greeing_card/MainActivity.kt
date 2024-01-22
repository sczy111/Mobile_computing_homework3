package com.example.greeing_card

import SettingsView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.greeing_card.ui.theme.GreeingcardTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreeingcardTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeView(navController,this@MainActivity) }
                    composable("conversation") {
                        Conversation(SampleData.conversationSample, navController, this@MainActivity)
                    }
                    //composable("settings") { SettingsView(navController) }
                }
                }
            }
        }
    }


data class GreetingCard(val name:String, val message:String)


/*@Preview(name="Light Mode")
@Composable
fun GreetingPreviewLight() {
    GreeingcardTheme {
        Surface {
            Greeting(GreetingCard("Y", 22))
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name="Dark Mode")
@Composable
fun GreetingPreviewDark() {
    GreeingcardTheme {
        Surface {
            Greeting(GreetingCard("Y", 22))
        }
    }
}*/
