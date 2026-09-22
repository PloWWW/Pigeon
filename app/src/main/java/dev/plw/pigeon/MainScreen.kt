package dev.plw.pigeon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.plw.pigeon.network.NetworkController

@Composable
fun MainScreen(modifier: Modifier){

    val NWC = NetworkController()

    Scaffold(
        modifier
    ) {
        innerPadding->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {NWC.startServer()}
            ){
                Text("Start Sever")
            }
            Button(
                onClick = {NWC.stopServer()}
            ){
                Text("Stop Sever")
            }
        }
    }
}