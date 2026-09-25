package dev.plw.pigeon

import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.plw.pigeon.network.NetworkController
import dev.plw.pigeon.ui.components.Card
import dev.plw.pigeon.ui.components.ChooseFileCard
import dev.plw.pigeon.ui.components.SaveFileCard
import dev.plw.pigeon.ui.components.ServerStatusCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val clipboardManager = LocalClipboardManager.current
    val nwc = remember { NetworkController(context) }
    val scope = rememberCoroutineScope()

    val portFieldState = rememberTextFieldState("8080")
    var activePort by remember { mutableStateOf(8080) }
    var isServerRunning by remember { mutableStateOf(false) }

    val receivedFiles = remember { mutableStateListOf<File>() }
    val localIp = remember { nwc.getLocalIpAddress() } ?: "127.0.0.1"

    fun restartWithPort(newPort: Int) {
        scope.launch {
            isServerRunning = false
            nwc.stopServer()
            delay(200)
            nwc.startServer(port = newPort)
            activePort = newPort
            isServerRunning = true
        }
    }

    LaunchedEffect(Unit) {
        nwc.onFileReceived = { file ->
            scope.launch(Dispatchers.Main) {
                if (receivedFiles.none { it.name == file.name }) {
                    receivedFiles.add(file)
                }
            }
        }
        nwc.startServer(port = activePort)
        isServerRunning = true
    }

    val qrBackground = MaterialTheme.colorScheme.primary.toArgb()
    val qrForeground = MaterialTheme.colorScheme.surface.toArgb()

    val qrBitmap = remember(localIp, activePort) {
        try {
            val qrgEncoder = QRGEncoder("http://$localIp:$activePort", null, QRGContents.Type.TEXT, 512)
            qrgEncoder.setColorBlack(qrForeground)
            qrgEncoder.setColorWhite(qrBackground)
            qrgEncoder.getBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ServerStatusCard(
                isRunning = isServerRunning,
                port = activePort,
                onRestart = { restartWithPort(activePort) }
            )

            Card {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(250.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "http://$localIp:$activePort",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    IconButton(
                        { clipboardManager.setText(AnnotatedString("http://$localIp:$activePort"))}
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            "Copy",
                            Modifier.size(16.dp)
                        )
                    }
                }
            }

            ChooseFileCard(onFilesUpdated = { updatedList ->
                nwc.filesList = updatedList
            })

            SaveFileCard(receivedFiles = receivedFiles)

            Card {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Port",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TextField(
                        modifier = Modifier.width(90.dp),
                        state = portFieldState,
                        placeholder = { Text("Port") },
                        inputTransformation = InputTransformation.maxLength(5)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val parsed = portFieldState.text.toString().toIntOrNull()
                        val validPort = if (parsed != null && parsed in 1024..65535) parsed else 8080
                        restartWithPort(validPort)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Apply & Restart",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}