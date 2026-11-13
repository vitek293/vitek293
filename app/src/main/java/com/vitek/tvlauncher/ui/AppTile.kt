package com.vitek.tvlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vitek.tvlauncher.data.TvApp

private val TileShape = RoundedCornerShape(26.dp)
private val TopShelfShape = RoundedCornerShape(30.dp)

/** Long-press threshold for the D-pad center key, in milliseconds. */
private const val LONG_PRESS_MS = 500L

/**
 * tvOS-style square app tile: rounded icon, label below.
 * The focused tile scales up and lifts with a soft shadow.
 */
@Composable
fun AppTile(
    app: TvApp,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.16f else 1f,
        animationSpec = tween(160),
    )
    var downTime by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier.zIndex(if (focused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (focused) 28.dp else 10.dp,
                    shape = TileShape,
                )
                .clip(TileShape)
                .background(Color.White)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                    onFocusChange(it.isFocused)
                }
                .onKeyEvent { event ->
                    when {
                        event.key == Key.DirectionCenter && event.type == KeyEventType.KeyDown -> {
                            downTime = event.nativeKeyEvent.eventTime
                            true
                        }
                        event.key == Key.DirectionCenter && event.type == KeyEventType.KeyUp -> {
                            if (event.nativeKeyEvent.eventTime - downTime > LONG_PRESS_MS) {
                                onLongClick()
                            } else {
                                onClick()
                            }
                            true
                        }
                        else -> false
                    }
                }
                .focusable()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            if (focused) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.14f)))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = app.label,
            color = if (focused) Color.White else Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(128.dp),
        )
    }
}

/** Wide "Top Shelf" tile for featured apps, with a gradient card behind the icon. */
@Composable
fun TopShelfTile(
    app: TvApp,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(160),
    )
    var downTime by remember { mutableLongStateOf(0L) }

    Row(
        modifier = Modifier
            .size(width = 360.dp, height = 200.dp)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (focused) 32.dp else 12.dp,
                shape = TopShelfShape,
            )
            .clip(TopShelfShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF262B3D), Color(0xFF14161F)),
                ),
            )
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = if (focused) 0.65f else 0f),
                shape = TopShelfShape,
            )
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .onKeyEvent { event ->
                when {
                    event.key == 
