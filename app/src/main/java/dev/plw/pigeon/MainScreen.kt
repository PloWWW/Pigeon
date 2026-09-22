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
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import dev.plw.pigeon.network.NetworkController

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    val nwc = remember { NetworkController() }
    val localIp = remember { nwc.getLocalIpAddress() } ?: "127.0.0.1"

    LaunchedEffect(Unit) {
        nwc.startServer()
    }

    val qrBitmap = remember(localIp) {
        try {
            val qrgEncoder = QRGEncoder("http://$localIp:8080", null, QRGContents.Type.TEXT, 512)
            qrgEncoder.setColorBlack(Color.BLACK)
            qrgEncoder.setColorWhite(Color.WHITE)
            qrgEncoder.getBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(250.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "IP: http://$localIp:8080")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { nwc.stopServer() }
            ) {
                Text("Stop Server")
            }
        }
    }
}