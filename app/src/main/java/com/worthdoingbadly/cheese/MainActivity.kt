package com.worthdoingbadly.cheese

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.worthdoingbadly.cheese.ui.theme.CheeseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean

// https://stackoverflow.com/a/78930945
private fun InputStream.toLineFlow() = bufferedReader(StandardCharsets.UTF_8).lineSequence().asFlow().onCompletion {close()}

private const val CHEESE_INITIAL_MESSAGE = """
Cheese v2025-08-10
Press Go to temp root with Magisk.
Your device will restart.

Warning:
This disables all security on your device.
When temp rooted, do NOT run any apps or browse any websites you don't trust.
Do NOT flash to the boot partition: Magisk's Direct Install won't work.

CVE-2025-21479 temp root by Zhuowei and the developers at XRBreak and FreeXR.

Contains code from:
 - adrenaline by Project Zero
 - adreno_user from m-y-mo
 - Freedreno from Mesa
 - shellcode from Longterm Security
 
For more information, click Help.
"""

class MainActivity : ComponentActivity() {
    private val consoleText = mutableStateOf(CHEESE_INITIAL_MESSAGE)
    private val scrollState = ScrollState(initial = 0)
    private val running = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheeseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Text(consoleText.value, modifier = Modifier.verticalScroll(scrollState).weight(1f))
                        Row(modifier = Modifier.height(80.dp)) {
                            Button(
                                onClick = { runCheese() },
                                enabled = !running.value,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                Text("Go!")
                            }
                            Button(
                                onClick = { openHelp() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxHeight(),
                                ) {
                                Text("Help")
                            }
                            Button(
                                onClick = { copyText() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Text("Copy")
                            }
                        }
                    }
                }
            }
        }
    }
    private fun runCheese() {
        consoleText.value = "Starting...\n"
        running.value = true
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
            val processBuilder = ProcessBuilder().command(executablePath, "sh", launchShPath)
                .redirectErrorStream(true)
            val process = processBuilder.start()

            launch {
                process.inputStream.toLineFlow()
                    .collect { line ->
                        println(line)
                        launch(Dispatchers.Main) {
                            consoleText.value += line + "\n"
                            scrollState.scrollTo(100000)
                        }
                    }
            }
            // todo dump output
            val returnVal = process.waitFor()
            println("cheese returned $returnVal")
            launch(Dispatchers.Main) {
                consoleText.value += "cheese returned $returnVal" + "\n"
                scrollState.scrollTo(100000)
                running.value = false
            }
        }
    }
    private fun openHelp() {
        val intent = packageManager.getLaunchIntentForPackage("com.oculus.vrshell")!!
        intent.putExtra("intent_data", "systemux://browser");
        intent.putExtra("uri", Uri.parse("https://github.com/zhuowei/cheese"))
        startActivity(intent)
    }
    private fun copyText() {
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        // When setting the clipboard text.
        clipboardManager.setPrimaryClip(ClipData.newPlainText   ("", consoleText.value))
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