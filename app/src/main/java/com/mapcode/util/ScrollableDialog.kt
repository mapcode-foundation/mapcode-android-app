package com.mapcode.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ScrollableDialog(onDismiss: () -> Unit, title: String, buttonText: String, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = MaterialTheme.colors.surface, shape = MaterialTheme.shapes.medium) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .height(64.dp)
                        .wrapContentSize()
                        .padding(start = 24.dp, end = 24.dp),
                    text = title,
                    style = MaterialTheme.typography.h6
                )
                Divider(Modifier.height(1.dp))
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    content()
                }
                Divider(Modifier.height(1.dp))
                TextButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(8.dp),
                    onClick = onDismiss
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}