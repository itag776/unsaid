package com.example.unsaid

import androidx.compose.ui.zIndex
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unsaid.ui.theme.InkCharcoal
import com.example.unsaid.ui.theme.InterFont
import com.example.unsaid.ui.theme.LibreFont
import com.example.unsaid.ui.theme.PaperWhite
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- CONFIGURATION (PASTE YOUR KEYS HERE) ---
const val SUPABASE_URL = "https://ukgrxtvfcngbpktgrpin.supabase.co"
const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVrZ3J4dHZmY25nYnBrdGdycGluIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU4MjA0MjAsImV4cCI6MjA4MTM5NjQyMH0.UUOs8evp09quy97Z5TUFSs5p3LWWvnyRzn2toGZPp7U"

// --- SUPABASE CLIENT ---
val supabase = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_KEY
) {
    install(Postgrest)
}

// --- DATA MODEL ---
@Serializable
data class Letter(
    val id: Long = 0,
    val recipient: String,
    val message: String,
    val space: String,
    @SerialName("color_hex") val colorHex: Long,
    @SerialName("created_at") val createdAt: String? = null
)

// --- ROUTES ---
object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val CHOOSE_SPACE = "choose_space"
    const val VERIFY = "verify"
    const val WRITE = "write"
    const val FEED_WITH_ARG = "feed/{spaceName}"
}

// --- BOUNCY MODIFIER ---
fun Modifier.bounceClick(scaleDown: Float = 0.95f, onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    val scale by animateFloatAsState(if (isPressed) scaleDown else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    LaunchedEffect(isPressed) { if (isPressed) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
    this.graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

// --- HELPER: TIME AGO ---
fun getTimeAgo(isoString: String?): String {
    if (isoString == null) return "Just now"
    try {
        val instant = java.time.Instant.parse(isoString)
        val now = java.time.Instant.now()
        val diff = java.time.Duration.between(instant, now)

        return when {
            diff.toMinutes() < 1 -> "Just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
            diff.toHours() < 24 -> "${diff.toHours()}h ago"
            else -> "${diff.toDays()}d ago"
        }
    } catch (e: Exception) {
        return "Recently"
    }
}

// --- MAIN NAVIGATION ---
@Composable
fun UnsaidNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen { navController.navigate(Routes.WELCOME) { popUpTo(Routes.SPLASH) { inclusive = true } } }
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onReadClick = { navController.navigate(Routes.CHOOSE_SPACE) },
                onWriteClick = { navController.navigate("${Routes.WRITE}/Global") }
            )
        }

        composable(Routes.CHOOSE_SPACE) {
            ChooseSpaceScreen(
                onGlobalClick = { navController.navigate("feed/Global") },
                onCollegeClick = { navController.navigate(Routes.VERIFY) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.VERIFY) {
            VerificationScreen(
                onVerificationSuccess = { navController.navigate("feed/College") { popUpTo(Routes.CHOOSE_SPACE) } },
                onBackClick = { navController.popBackStack() }
            )
        }

        // FEED
        composable(
            route = Routes.FEED_WITH_ARG,
            arguments = listOf(navArgument("spaceName") { type = NavType.StringType })
        ) { backStackEntry ->
            val spaceName = backStackEntry.arguments?.getString("spaceName") ?: "Global"

            var letters by remember { mutableStateOf<List<Letter>>(emptyList()) }
            var isLoading by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            // LOGIC: Fetch Letters
            val refreshLetters = {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        letters = supabase.from("letters")
                            .select {
                                filter { eq("space", spaceName) }
                                order("created_at", Order.DESCENDING)
                            }.decodeList<Letter>()
                    } catch (e: Exception) {
                        println("Error: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            }

            // Load on first open
            LaunchedEffect(spaceName) {
                refreshLetters()
            }

            FeedScreen(
                spaceName = spaceName,
                letters = letters,
                isLoading = isLoading,
                onHeaderClick = { navController.popBackStack() },
                onWriteClick = { navController.navigate("${Routes.WRITE}/$spaceName") },
                onRefresh = { refreshLetters() }
            )
        }

        // WRITE
        composable(
            route = "${Routes.WRITE}/{spaceName}",
            arguments = listOf(navArgument("spaceName") { type = NavType.StringType })
        ) { backStackEntry ->
            val spaceName = backStackEntry.arguments?.getString("spaceName") ?: "Global"
            val scope = rememberCoroutineScope()

            WriteScreen(
                spaceName = spaceName,
                onBackClick = { navController.popBackStack() },
                onPostClick = { newLetter ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            supabase.from("letters").insert(newLetter)
                            withContext(Dispatchers.Main) {
                                navController.popBackStack()
                            }
                        } catch(e: Exception) {
                            println("Error uploading: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

// --- SCREENS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    spaceName: String,
    letters: List<Letter>,
    isLoading: Boolean,
    onHeaderClick: () -> Unit,
    onWriteClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        containerColor = PaperWhite,
        floatingActionButton = { FloatingActionButton(onClick = { onWriteClick() }, containerColor = InkCharcoal, contentColor = Color.White) { Icon(Icons.Default.Edit, contentDescription = "Write") } }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Loading Bar at Top
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(1f), color = InkCharcoal)
            }

            LazyColumn(contentPadding = PaddingValues(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 100.dp), modifier = Modifier.fillMaxSize()) {

                // HEADER with Refresh Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back Button + Title
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onHeaderClick() }.padding(8.dp).offset(x = (-8).dp)) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = spaceName, fontFamily = InterFont, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkCharcoal)
                        }

                        // NEW: Refresh Button
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = InkCharcoal)
                        }
                    }
                }

                items(letters) { letter ->
                    val paperColor = Color(letter.colorHex)
                    val textColor = if (paperColor.toArgb() == Color(0xFF2D2D2D).toArgb()) Color.White else InkCharcoal
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = paperColor), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("To: ${letter.recipient}", fontFamily = InterFont, fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                                Text(text = getTimeAgo(letter.createdAt), fontFamily = InterFont, fontSize = 12.sp, color = textColor.copy(alpha = 0.4f))
                            }
                            Text(letter.message, fontFamily = LibreFont, fontSize = 16.sp, lineHeight = 24.sp, color = textColor)
                        }
                    }
                }
            }

            // Empty State
            if (letters.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No letters yet.", fontFamily = InterFont, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); onTimeout() }
    Box(modifier = Modifier.fillMaxSize().background(PaperWhite), contentAlignment = Alignment.Center) {
        Text("Unsaid.", color = InkCharcoal, fontSize = 32.sp, fontFamily = LibreFont)
    }
}

@Composable
fun WelcomeScreen(onReadClick: () -> Unit, onWriteClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(PaperWhite).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Unsaid.", fontFamily = LibreFont, fontSize = 48.sp, color = InkCharcoal, modifier = Modifier.padding(bottom = 16.dp))
            Text("For the words you never said.", fontFamily = InterFont, fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 64.dp))
            Box(modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(0.1f)).clip(RoundedCornerShape(12.dp)).background(InkCharcoal).bounceClick(onClick = onReadClick), contentAlignment = Alignment.Center) {
                Text("Read Letters", fontFamily = InterFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(56.dp).border(1.5.dp, InkCharcoal, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).bounceClick(onClick = onWriteClick), contentAlignment = Alignment.Center) {
                Text("Write a Letter", fontFamily = InterFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkCharcoal)
            }
        }
        Text("Anonymous. Forever.", fontFamily = InterFont, fontSize = 12.sp, color = Color.LightGray, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp))
    }
}

@Composable
fun ChooseSpaceScreen(onGlobalClick: () -> Unit, onCollegeClick: () -> Unit, onBackClick: () -> Unit) {
    Scaffold(
        containerColor = PaperWhite,
        topBar = { IconButton(onClick = onBackClick, modifier = Modifier.padding(top = 48.dp, start = 16.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = InkCharcoal) } }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Choose Space", fontFamily = InterFont, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkCharcoal, modifier = Modifier.padding(bottom = 48.dp))
            PremiumSelectionCard("Global", "Visible to everyone", Icons.Default.Home, onGlobalClick)
            Spacer(modifier = Modifier.height(16.dp))
            PremiumSelectionCard("College", "Visible only within your college", Icons.Default.Person, onCollegeClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(onVerificationSuccess: () -> Unit, onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = PaperWhite,
        topBar = {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(top = 48.dp, start = 16.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = InkCharcoal)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("College Verification", fontFamily = InterFont, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkCharcoal, modifier = Modifier.padding(bottom = 16.dp))
            Text("Verify your college email to access the college space.", fontFamily = InterFont, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))
            Text("College Email", fontFamily = InterFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkCharcoal, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            TextField(value = email, onValueChange = { email = it }, placeholder = { Text("you@college.edu", color = Color.LightGray) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)))
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(if (email.isNotEmpty()) InkCharcoal else Color.LightGray).bounceClick { if (email.isNotEmpty() && !isLoading) { isLoading = true; scope.launch { delay(2000); isLoading = false; onVerificationSuccess() } } }, contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Send Verification Link", fontFamily = InterFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun PremiumSelectionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp).shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.05f)).clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(16.dp)).bounceClick(onClick = onClick).padding(24.dp)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(PaperWhite), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = InkCharcoal) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontFamily = InterFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = InkCharcoal)
                Text(description, fontFamily = InterFont, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(spaceName: String, onBackClick: () -> Unit, onPostClick: (Letter) -> Unit) {
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFF5F5F5)) }
    val isDarkPaper = selectedColor == Color(0xFF2D2D2D)
    val textColor = if (isDarkPaper) Color.White else InkCharcoal
    val hintColor = if (isDarkPaper) Color.LightGray else Color.Gray

    Scaffold(
        containerColor = PaperWhite,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = InkCharcoal) }
                Text("$spaceName Space", fontFamily = InterFont, color = Color.Gray)
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(PaperWhite).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(0.2f)).clip(RoundedCornerShape(12.dp)).background(if (message.isNotEmpty()) InkCharcoal else Color.LightGray).bounceClick(onClick = {
                    if (message.isNotEmpty()) {
                        val newLetter = Letter(recipient = if (recipient.isEmpty()) "Anonymous" else recipient, message = message, space = spaceName, colorHex = selectedColor.toArgb().toLong())
                        onPostClick(newLetter)
                    }
                }), contentAlignment = Alignment.Center) {
                    Text("Post Letter", fontFamily = InterFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5), Color(0xFFEBE7DF), Color(0xFF2D2D2D)).forEach { color ->
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color).border(if (selectedColor == color) 2.dp else 1.dp, if (selectedColor == color) InkCharcoal else Color.LightGray, CircleShape).clickable { selectedColor = color })
                }
            }
            Card(modifier = Modifier.fillMaxWidth().height(400.dp), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = selectedColor), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("To: ", fontFamily = InterFont, color = hintColor)
                        TextField(value = recipient, onValueChange = { recipient = it }, placeholder = { Text("...", color = hintColor.copy(0.5f)) }, textStyle = TextStyle(fontFamily = InterFont, fontSize = 16.sp, color = textColor), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = textColor, focusedTextColor = textColor, unfocusedTextColor = textColor), modifier = Modifier.fillMaxWidth())
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(hintColor.copy(0.1f)).padding(vertical = 8.dp))
                    TextField(value = message, onValueChange = { message = it }, placeholder = { Text("Write what you never said...", color = hintColor.copy(0.5f)) }, textStyle = TextStyle(fontFamily = LibreFont, fontSize = 18.sp, color = textColor), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = textColor, focusedTextColor = textColor, unfocusedTextColor = textColor), modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}