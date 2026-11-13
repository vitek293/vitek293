package com.vitek.tvlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vitek.tvlauncher.LauncherViewModel
import com.vitek.tvlauncher.data.TvApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private data class SystemAction(
    val title: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val onClick: () -> Unit,
)

/** Section indexes used for the background parallax. */
private const val SECTION_TOP = 0
private const val SECTION_APPS = 1
private const val SECTION_SYSTEM = 2

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val apps by viewModel.apps.collectAsState()
    val pins by viewModel.pins.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    var focusedSection by remember { mutableIntStateOf(SECTION_TOP) }
    var dialogApp by remember { mutableStateOf<TvApp?>(null) }
    var restoreFocusPkg by remember { mutableStateOf<String?>(null) }
    var initialFocusDone by remember { mutableStateOf(false) }

    // One requester per tile; the section prefix keeps keys unique, since an app
    // can appear both in the Top Shelf row and in the "all apps" row.
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(key: String): FocusRequester =
        focusRequesters.getOrPut(key) { FocusRequester() }

    val listState = rememberLazyListState()

    val featured = remember(apps, pins) {
        val pinned = pins.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
        if (pinned.isNotEmpty()) pinned else apps.take(4)
    }

    // Refresh the app list whenever we come back to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial focus: the first Top Shelf tile (or the first app).
    LaunchedEffect(featured, apps) {
        if (!initialFocusDone) {
            val target = featured.firstOrNull() ?: apps.firstOrNull()
            if (target != null) {
                requesterFor("top:${target.packageName}").requestFocus()
                initialFocusDone = true
            }
        }
    }

    // Restore focus to the tile we came from after the context dialog closes.
    LaunchedEffect(restoreFocusPkg) {
        val pkg = restoreFocusPkg
        if (pkg != null) {
            restoreFocusPkg = null
            val target = (featured.firstOrNull() ?: apps.firstOrNull())?.packageName
            val key = when {
                featured.any { it.packageName == pkg } -> "top:$pkg"
                target != null -> "top:$target"
                else -> null
            }
            if (key != null) requesterFor(key).requestFocus()
        }
    }

    fun launchApp(app: TvApp) {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openDialog(app: TvApp) {
        dialogApp = app
    }

    fun dismissDialog(restorePkg: String) {
        dialogApp = null
        restoreFocusPkg = restorePkg
    }

    val systemActions = remember(context) {
        buildList {
            add(
                SystemAction(
                    title = "Настройки",
                    icon = Icons.Filled.Settings,
                    gradient = listOf(Color(0xFF4A4F5E), Color(0xFF2A2E3A)),
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                    )
                },
            )
            add(
                SystemAction(
                    title = "Дом",
                    icon = Icons.Filled.Home,
                    gradient = listOf(Color(0xFF8A5CD1), Color(0xFF4A2E82)),
                ) {
                    // Lets you pick/clear the default home app without digging
                    // through the settings tree — handy on Xiaomi boxes.
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            },
                        )
                    }
                },
            )
            add(
                SystemAction(
                    title = "Поиск",
                    icon = Icons.Filled.Search,
                    gradient = listOf(Color(0xFF3B6FD4), Color(0xFF1E3A6E)),
                ) {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_WEB_SEARCH).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            },
                        )
                    } catch (e: Exception) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            },
                        )
                    }
                },
            )
            val storeIntent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
            if (storeIntent != null) {
                add(
                    SystemAction(
                        title = "Play Маркет",
                        icon = Icons.Filled.PlayArrow,
                        gradient = listOf(Color(0xFF3FA46A), Color(0xFF1E5B3A)),
                    ) {
                        storeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(storeIntent)
                    },
                )
            }
        }
    }

    // Subtle parallax: background glows drift as the focused row changes.
    val bgShift by animateDpAsState(
        targetValue = (focusedSection * 18).dp,
        animationSpec = tween(700),
    )
    val topGlow = remember(density) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF2B3A8F).copy(alpha = 0.35f), Color.Transparent),
            radius = with(density) { 380.dp.toPx() },
        )
    }
    val bottomGlow = remember(density) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF0E5F56).copy(alpha = 0.28f), Color.Transparent),
            radius = with(density) { 420.dp.toPx() },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05060A)),
    ) {
        // Background: vertical gradient + two soft glows with parallax.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B0E1A), Color(0xFF05060A)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(760.dp)
                .offset(x = (-180).dp, y = (-240).dp + bgShift)
                .background(topGlow),
        )
        Box(
            modifier = Modifier
                .size(840.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 220.dp, y = (220.dp - bgShift))
                .background(bottomGlow),
        )

        val currentDialogApp = dialogApp
        if (currentDialogApp != null) {
            AppContextDialog(
                app = currentDialogApp,
                isPinned = viewModel.isPinned(currentDialogApp.packageName),
                onDismiss = { dismissDialog(currentDialogApp.packageName) },
                onTogglePin = {
                    viewModel.togglePin(currentDialogApp.packageName)
                    dismissDialog(currentDialogApp.packageName)
                },
                onUninstall = {
                    dialogApp = null
                    restoreFocusPkg = currentDialogApp.packageName
                    context.startActivity(
                        Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse("package:${currentDialogApp.packageName}"),
                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
                    )
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 56.dp, vertical = 36.dp),
            ) {
                Header()
                Spacer(Modifier.height(24.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(36.dp),
                    contentPadding = PaddingValues(bottom = 48.dp),
                ) {
                    if (featured.isNotEmpty()) {
                        item {
                            SectionTitle("Топ")
                            Spacer(Modifier.height(16.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(end = 48.dp),
                            ) {
                                items(featured, key = { "top:" + it.packageName }) { app ->
                                    TopShelfTile(
                                        app = app,
                                        focusRequester = requesterFor("top:${app.packageName}"),
                                        onFocusChange = { if (it) focusedSection = SECTION_TOP },
                                        onClick = { launchApp(app) },
                                        onLongClick = { openDialog(app) },
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionTitle("Приложения")
                        Spacer(Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            contentPadding = PaddingValues(end = 48.dp),
                        ) {
                            items(apps, key = { "app:" + it.packageName }) { app ->
                                AppTile(
                                    app = app,
                                    focusRequester = requesterFor("app:${app.packageName}"),
                                    onFocusChange = { if (it) focusedSection = SECTION_APPS },
                                    onClick = { launchApp(app) },
                                    onLongClick = { openDialog(app) },
                                )
                            }
                        }
                    }

                    item {
                        SectionTitle("Система")
                        Spacer(Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            contentPadding = PaddingValues(end = 48.dp),
                        ) {
                            items(systemActions, key = { "sys:" + it.title }) { action ->
                                SystemTile(
                                    title = action.title,
                                    icon = action.icon,
                                    gradient = action.gradient,
                                    focusRequester = requesterFor("sys:${action.title}"),
                                    onFocusChange = { if (it) focusedSection = SECTION_SYSTEM },
                                    onClick = action.onClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val locale = Locale.getDefault()
    val timeText = remember(nowMillis) {
        SimpleDateFormat("HH:mm", locale).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEEE, d MMMM", locale).format(Date(nowMillis))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Главная",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = dateText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
