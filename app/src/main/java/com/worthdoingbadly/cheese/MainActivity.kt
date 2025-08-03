package com.worthdoingbadly.cheese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.worthdoingbadly.cheese.ui.theme.CheeseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// https://stackoverflow.com/a/78930945
private fun InputStream.toLineFlow() = bufferedReader(StandardCharsets.UTF_8).lineSequence().asFlow().onCompletion {close()}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheeseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                    Button(onClick = { runCheese() }) {
                        Text("Go!")
                    }
                }
            }
        }
    }
    private fun runCheese() {
        lifecycleScope.launch(Dispatchers.IO) {
            println("im in")
            val assetManager = assets
            val extractedDir = getDir("cheese", 0)
            for (filename in assetManager.list("cheese")!!) {
                assetManager.open("cheese/" + filename).use {
                    val targetFile = File(extractedDir, filename)
                    Files.copy(it, targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                    targetFile.setExecutable(true)
                }
            }
            val executablePath = applicationInfo.nativeLibraryDir + "/libcheese.so"
            val launchShPath = File(extractedDir, "launch.sh").path
            val process = Runtime.getRuntime().exec(arrayOf(executablePath, "sh", launchShPath))

            launch {
                process.inputStream.toLineFlow()
                    .collect { line ->
                        launch(Dispatchers.Main) {
                            println(line)
                        }
                    }
            }
            launch {
                process.errorStream.toLineFlow()
                    .collect { line ->
                        launch(Dispatchers.Main) {
                            println(line)
                        }
                }
            }
            // todo dump output
            val returnVal = process.waitFor()
            println("cheese returned $returnVal")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CheeseTheme {
        Greeting("Android")
    }
}