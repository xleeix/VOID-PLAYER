package com.voidplayer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.net.Uri

private val Ink = Color(0xFF080510)
private val Violet = Color(0xFF9B4DFF)
private val Purple = Color(0xFF6424B8)
private val Cyan = Color(0xFF75DFFF)
private val Muted = Color(0xFFB9AEC9)
private val Glass = Color(0x24FFFFFF)
private val Stroke = Color(0x38FFFFFF)

data class Track(val id: Long, val title: String, val artist: String, val uri: Uri, val duration: Long)

class MainActivity : ComponentActivity() {
    private var onPermissionResult: (() -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        onPermissionResult?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var tracks by remember { mutableStateOf(emptyList<Track>()) }
            var selected by remember { mutableStateOf<Track?>(null) }
            var tab by remember { mutableStateOf("Home") }
            var query by remember { mutableStateOf("") }

            fun scan() {
                val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                    else Manifest.permission.READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    onPermissionResult = { tracks = loadTracks() }
                    permissionLauncher.launch(permission)
                } else tracks = loadTracks()
            }

            LaunchedEffect(Unit) { scan() }

            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Color(0xFF171020), primary = Violet,
                onBackground = Color.White, onSurface = Color.White
            )) {
                VoidApp(
                    tracks = tracks,
                    selected = selected,
                    tab = tab,
                    query = query,
                    onTab = { tab = it },
                    onQuery = { query = it },
                    onRefresh = { scan() },
                    onSelect = { selected = it }
                )
            }
        }
    }

    private fun loadTracks(): List<Track> {
        val result = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result += Track(
                    id, c.getString(titleCol) ?: "Unknown title",
                    c.getString(artistCol) ?: "Unknown artist",
                    Uri.withAppendedPath(collection, id.toString()),
                    c.getLong(durationCol)
                )
            }
        }
        return result
    }
}

@Composable
private fun VoidApp(
    tracks: List<Track>, selected: Track?, tab: String, query: String,
    onTab: (String) -> Unit, onQuery: (String) -> Unit,
    onRefresh: () -> Unit, onSelect: (Track) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF160B28), Ink, Color.Black)))) {
        Box(Modifier.size(260.dp).offset(x = 170.dp, y = (-70).dp).blur(90.dp).background(Purple.copy(.34f), CircleShape))
        Box(Modifier.size(220.dp).align(Alignment.BottomStart).offset(x = (-80).dp, y = 40.dp).blur(90.dp).background(Violet.copy(.22f), CircleShape))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("VOID", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("PLAYER", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                }
                GlassIcon(Icons.Default.Refresh, onRefresh)
                Spacer(Modifier.width(10.dp))
                GlassIcon(Icons.Default.MoreVert) {}
            }
            when (tab) {
                "Home" -> HomeScreen(tracks, selected, onSelect)
                "Search" -> SearchScreen(tracks, query, onQuery, onSelect)
                "Library" -> LibraryScreen(tracks, onSelect)
                "Liked" -> EmptyFeature("Your favorites", "Your liked tracks will appear here.")
            }
            Spacer(Modifier.weight(1f))
            if (selected != null) MiniPlayer(selected)
            BottomBar(tab, onTab)
        }
    }
}

@Composable
private fun HomeScreen(tracks: List<Track>, selected: Track?, onSelect: (Track) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp)) {
        Text("Hello, music lover", color = Muted, fontSize = 14.sp)
        Text("Feel the music.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR SOUND. YOUR WORLD.", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Your music,\nbeautifully yours.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("${tracks.size} songs found on this device", color = Muted, fontSize = 12.sp)
                }
                Box(Modifier.size(82.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Violet, Purple, Cyan))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.size(45.dp))
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recently added", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("SEE ALL", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        if (tracks.isEmpty()) EmptyFeature("No music found", "Add audio files to your phone, then tap refresh.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(tracks.take(30), key = { it.id }) { track -> TrackRow(track, onSelect) }
        }
    }
}

@Composable
private fun SearchScreen(tracks: List<Track>, query: String, onQuery: (String) -> Unit, onSelect: (Track) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp)) {
        Text("Search", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Songs or artists…", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
            shape = RoundedCornerShape(22.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet, unfocusedBorderColor = Stroke,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = Cyan
            )
        )
        Spacer(Modifier.height(12.dp))
        val filtered = tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { TrackRow(it, onSelect) }
        }
    }
}

@Composable
private fun LibraryScreen(tracks: List<Track>, onSelect: (Track) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp)) {
        Text("Your library", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All songs", "Artists", "Albums", "Folders").forEach {
                Surface(color = Glass, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Stroke)) {
                    Text(it, Modifier.padding(horizontal = 15.dp, vertical = 9.dp), color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (tracks.isEmpty()) EmptyFeature("Your library is empty", "Allow audio access and add music to your device.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tracks, key = { it.id }) { TrackRow(it, onSelect) }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onSelect: (Track) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color(0x24FFFFFF)).border(1.dp, Stroke, RoundedCornerShape(18.dp))
            .clickable { onSelect(track) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Purple, Violet, Color(0xFF257EAA)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(29.dp))
    }
}

@Composable
private fun MiniPlayer(track: Track) {
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Violet, Cyan))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Album, null, tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(track.artist, color = Muted, fontSize = 11.sp, maxLines = 1)
            }
            Icon(Icons.Default.SkipPrevious, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.SkipNext, null, tint = Color.White)
        }
    }
}

@Composable
private fun EmptyFeature(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LibraryMusic, null, tint = Violet, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0x38FFFFFF), Color(0x14A76CFF), Color(0x1AFFFFFF))))
            .border(1.dp, Stroke, RoundedCornerShape(26.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun GlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(Glass).border(1.dp, Stroke, CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White)
    }
}

@Composable
private fun BottomBar(selected: String, onSelect: (String) -> Unit) {
    val items = listOf("Home" to Icons.Default.Home, "Search" to Icons.Default.Search, "Liked" to Icons.Default.Favorite, "Library" to Icons.Default.LibraryMusic)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp)).background(Color(0xCC20172D))
            .border(1.dp, Stroke, RoundedCornerShape(28.dp)).padding(7.dp),
        horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, icon) ->
            val active = selected == label
            val tint by animateColorAsState(if (active) Color.White else Muted, label = "navTint")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                    .background(if (active) Brush.linearGradient(listOf(Violet, Purple)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelect(label) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                Text(label, color = tint, fontSize = 9.sp)
            }
        }
    }
}
