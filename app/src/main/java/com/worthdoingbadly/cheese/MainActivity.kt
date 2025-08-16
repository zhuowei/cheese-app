package com.worthdoingbadly.cheese

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
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
Cheese v2025-08-16
Press Go to temp root with Magisk.
Your device will restart.

Warning:
This disables all security on your device.
When temp rooted, do NOT run any apps or browse any websites you don't trust.
Do NOT write to the boot or system partition. You will brick.
Do NOT use Magisk's Direct Install.
You may want to back up your deviceKey, Meta Access Token and Oculus Access Token after root: you can find a link to FreeXR's guide in Help.

CVE-2025-21479 temp root by Zhuowei and the developers at XRBreak and FreeXR.

Contains code from:
 - adrenaline by Project Zero
 - adreno_user from m-y-mo
 - Freedreno from Mesa
 - shellcode from Longterm Security
 - Magisk from topjohnwu and the Magisk developers.
 
For more information, click Help.
"""

private const val PATCHED_MESSAGE = """
Unfortunately, your device has been patched:
You're running version VERSION_CURRENT.
The last vulnerable version is VERSION_LAST.

For more information, click Help.
"""

class MainActivity : ComponentActivity() {
    private val consoleText = mutableStateOf(CHEESE_INITIAL_MESSAGE)
    private val scrollState = ScrollState(initial = 0)
    private val running = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isPatched()) {
            consoleText.value = makePatchedMessage()
        }
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

    private fun getFingerprint() = Build.FINGERPRINT

    private fun isPatched(): Boolean {
        val lastVersion = lastVersionForDevice()
        if (lastVersion == 0L) {
            return false // you're on your own
        }
        return fingerprintToBuildVersion(getFingerprint()) > lastVersion
    }

    private fun makePatchedMessage(): String = PATCHED_MESSAGE
        .replace("VERSION_CURRENT", formatBuildVersion(fingerprintToBuildVersion(getFingerprint())))
        .replace("VERSION_LAST", formatBuildVersion(lastVersionForDevice()))
}

private fun lastVersionForDevice(): Long = when(Build.BOARD) {
    "eureka" -> 51154110129000520L
    "panther" -> 1176880099000610L
    else -> 0
}

private fun fingerprintToBuildVersion(fingerprint: String) = fingerprint.split(":")[1].split("/")[2].toLong()

private fun formatBuildVersion(version: Long) = "" + (version / 1_000000_0000L) + "." + ((version / 1_0000) % 1_000000) + "." + (version % 1_0000)
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