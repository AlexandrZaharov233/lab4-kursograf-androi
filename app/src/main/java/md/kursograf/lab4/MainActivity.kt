package md.kursograf.lab4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import md.kursograf.lab4.ui.KursografApp
import md.kursograf.lab4.ui.KursografTheme

class MainActivity : ComponentActivity() {
    private val model: KursografViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            KursografTheme {
                KursografApp(state = state, onRefresh = model::refresh)
            }
        }
    }
}
