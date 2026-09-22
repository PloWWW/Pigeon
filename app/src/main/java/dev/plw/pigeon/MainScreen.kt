package dev.plw.pigeon

import android.graphics.Color
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.plw.pigeon.model.SharedFile
import dev.plw.pigeon.network.NetworkController
import dev.plw.pigeon.ui.components.Card
import dev.plw.pigeon.ui.components.ChooseFileCard
import dev.plw.pigeon.ui.components.ServerStatusCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current.applicationContext

    val nwc = remember { NetworkController(context) }
    val scope = rememberCoroutineScope()

    var isServerRunning by remember { mutableStateOf(false) }
    val localIp = remember { nwc.getLocalIpAddress() } ?: "127.0.0.1"

    LaunchedEffect(Unit) {
        nwc.startServer()
        isServerRunning = true
    }

    val qrBackground = MaterialTheme.colorScheme.primary.toArgb()
    val qrForeground = MaterialTheme.colorScheme.surface.toArgb()

    val qrBitmap = remember(localIp) {
        try {
            val qrgEncoder = QRGEncoder("http://$localIp:8080", null, QRGContents.Type.TEXT, 512)
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
            Spacer(modifier = Modifier.height(4.dp))

            ServerStatusCard(
                isRunning = isServerRunning,
                port = 8080,
                onRestart = {
                    scope.launch {
                        isServerRunning = false
                        nwc.stopServer()
                        delay(200)
                        nwc.startServer()
                        isServerRunning = true
                    }
                }
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "http://$localIp:8080",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            ChooseFileCard(onFilesUpdated = {
                updatedList -> nwc.filesList = updatedList
            })

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}