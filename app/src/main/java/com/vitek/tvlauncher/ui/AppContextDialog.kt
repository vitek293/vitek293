package com.vitek.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitek.tvlauncher.data.TvApp

/**
 * tvOS-style context sheet shown on long-press of an app tile:
 * pin/unpin in Top Shelf, uninstall, or cancel.
 */
@Composable
fun AppContextDialog(
    app: TvApp,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onUninstall: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .shadow(40.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1D1F28))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.FillBounds,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            DialogButton(
                text = if (isPinned) "Убрать из топа" else "Закрепить в топе",
                onClick = onTogglePin,
                autofocus = true,
            )
            DialogButton(
                text = "Удалить приложение",
                onClick = onUninstall,
                danger = true,
            )
            DialogButton(
                text = "Отмена",
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    autofocus: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (autofocus) requester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (focused) Color.White.copy(alpha = 0.14f) else Color.Transparent,
            )
            .focusable()
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.key == Key.DirectionCenter && event.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (danger) Color(0xFFFF6B6B) else Color.White,
            fontSize = 17.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
