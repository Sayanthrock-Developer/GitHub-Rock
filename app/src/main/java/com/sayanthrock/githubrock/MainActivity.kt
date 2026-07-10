package com.sayanthrock.githubrock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GitHubRockTheme { GitHubRockApp() } }
    }
}

private val Background = Color(0xFF0D1117)
private val Surface = Color(0xFF161B22)
private val Primary = Color(0xFF2F81F7)
private val Success = Color(0xFF3FB950)
private val Warning = Color(0xFFD29922)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@Composable
fun GitHubRockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Primary,
            background = Background,
            surface = Surface,
            onPrimary = Color.White,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            secondary = Success,
            tertiary = Warning
        ),
        content = content
    )
}

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("home", "Home", Icons.Default.Home),
    Destination("repositories", "Repositories", Icons.Default.Folder),
    Destination("builds", "Builds", Icons.Default.Build),
    Destination("downloads", "Downloads", Icons.Default.Download),
    Destination("profile", "Profile", Icons.Default.Person)
)

@Composable
private fun GitHubRockApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                destinations.forEach { destination ->
                    val selected = backStack?.destination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen() }
            composable("repositories") { ListScreen("Repositories", demoRepositories, Icons.Default.Folder) }
            composable("builds") { ListScreen("Builds", demoBuilds, Icons.Default.Build) }
            composable("downloads") { ListScreen("Downloads", demoDownloads, Icons.Default.Download) }
            composable("profile") { ProfileScreen() }
        }
    }
}

@Composable
private fun HomeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("GitHub Rock", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Demo mode • Safe, isolated sample data", color = Warning)
        }
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(48.dp), tint = Primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Sayanth Rock", fontWeight = FontWeight.Bold)
                        Text("API rate limit: 4,876 / 5,000", color = TextSecondary)
                    }
                }
            }
        }
        item { SectionTitle("Quick actions") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("New repo", Icons.Default.Add, Modifier.weight(1f))
                QuickAction("New issue", Icons.Default.BugReport, Modifier.weight(1f))
                QuickAction("Build APK", Icons.Default.Android, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Attention") }
        items(demoAttention) { item -> StatusRow(item.first, item.second, item.third) }
        item { SectionTitle("Recent repositories") }
        items(demoRepositories) { StatusRow(it, "Recently opened", Success) }
    }
}

@Composable
private fun ListScreen(title: String, entries: List<String>, icon: ImageVector) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        items(entries) { entry ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = Primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry, fontWeight = FontWeight.SemiBold)
                        Text("Demo data", color = TextSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Authentication", fontWeight = FontWeight.Bold)
                Text("GitHub Device Flow foundation is configured. Add a GitHub client ID in local.properties to enable live sign-in.", color = TextSecondary)
                Button(onClick = {}, enabled = false) { Text("Login with GitHub") }
                OutlinedButton(onClick = {}) { Text("Continue in demo mode") }
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Surface, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable private fun SectionTitle(text: String) = Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

@Composable
private fun StatusRow(title: String, subtitle: String, color: Color) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary)
            }
        }
    }
}

private val demoRepositories = listOf("SayanthRock/GitHub-Rock", "SayanthRock/Rock-Wedding", "SayanthRock/OTA_ROCK")
private val demoBuilds = listOf("GitHub-Rock • assembleDebug • queued", "Rock-Wedding • bundleRelease • success", "OTA_ROCK • assembleRelease • failed")
private val demoDownloads = listOf("github-rock-debug.apk • verified", "rock-wedding-release.aab • verified")
private val demoAttention = listOf(
    Triple("OTA_ROCK workflow failed", "assembleRelease • 8 minutes ago", Color(0xFFF85149)),
    Triple("2 pull requests need review", "Rock-Wedding", Warning),
    Triple("1 issue assigned to you", "GitHub-Rock", Primary)
)
