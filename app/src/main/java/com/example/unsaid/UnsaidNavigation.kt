package com.example.unsaid

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.unsaid.ui.theme.InkCharcoal
import com.example.unsaid.ui.theme.InterFont
import com.example.unsaid.ui.theme.LibreFont
import com.example.unsaid.ui.theme.PaperWhite

// --- THEME DEFINITIONS ---
val TextGray = Color(0xFF666666)
val Graphite = Color(0xFF202020) // Premium Soft Black


// --- CONFIGURATION ---
const val SUPABASE_URL = "https://ukgrxtvfcngbpktgrpin.supabase.co"
// NOTE: For production, move this key to local.properties or BuildConfig
const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVrZ3J4dHZmY25nYnBrdGdycGluIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU4MjA0MjAsImV4cCI6MjA4MTM5NjQyMH0.UUOs8evp09quy97Z5TUFSs5p3LWWvnyRzn2toGZPp7U"

val supabase = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_KEY
) {
    install(Postgrest)
    install(Auth) {
        // DISABLE Persistence: Session is lost when app closes
        // (Note: In a real production app, you might want a "Remember Me" checkbox,
        // but this achieves exactly what you asked for: auto-logout on close).
        // If the library doesn't support 'alwaysSessionSave = false' easily,
        // we force logout on App Start instead.
    }
}
// FORCE LOGOUT ON START: Add this to your Main Activity or top of Navigation
// A cleaner way for "Logout on Close" without complex config:
// We will simply NOT check for a session at startup in the UI logic.

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
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

    const val FOCUS = "focus/{message}/{recipient}/{date}/{color}"

    const val CHOOSE_WRITE_SPACE = "choose_write_space"
    const val VERIFY = "verify"
    const val WRITE = "write"
    const val FEED_WITH_ARG = "feed/{spaceName}"
}

// --- BOUNCY MODIFIER ---
fun Modifier.bounceClick(scaleDown: Float = 0.95f, onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    val scale by animateFloatAsState(
        if (isPressed) scaleDown else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    LaunchedEffect(isPressed) {
        if (isPressed) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
            SplashScreen {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onReadClick = { navController.navigate(Routes.CHOOSE_SPACE) },
                onWriteClick = { navController.navigate(Routes.CHOOSE_WRITE_SPACE) }
            )
        }

        composable(Routes.CHOOSE_SPACE) {
            ChooseSpaceScreen(
                onGlobalClick = { navController.navigate("feed/Global") },
                onCollegeClick = {
                    // --- THE FIX: SESSION CHECK ---
                    val currentUser = supabase.auth.currentUserOrNull()

                    if (currentUser != null) {
                        // 1. User is already logged in -> Go straight to their feed
                        val domain = currentUser.email?.substringAfter("@") ?: "college"
                        navController.navigate("feed/$domain")
                    } else {
                        // 2. No user found -> Go to Verification
                        navController.navigate(Routes.VERIFY)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        // --- NEW: CHOOSE SPACE FOR WRITING ---
        composable(Routes.CHOOSE_WRITE_SPACE) {
            ChooseSpaceScreen(
                // 1. Global Button -> Goes to Write Global
                onGlobalClick = { navController.navigate("${Routes.WRITE}/Global") },

                // 2. College Button -> Check Login -> Go to Write College
                onCollegeClick = {
                    val currentUser = supabase.auth.currentUserOrNull()
                    if (currentUser != null) {
                        val domain = currentUser.email?.substringAfter("@") ?: "college"
                        navController.navigate("${Routes.WRITE}/$domain")
                    } else {
                        // If not logged in, they must verify first.
                        // (Note: After verifying, they will land on Feed, which is okay for now)
                        navController.navigate(Routes.VERIFY)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.FOCUS,
            arguments = listOf(
                navArgument("message") { type = NavType.StringType },
                navArgument("recipient") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("color") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            // This extracts the data passed from the feed
            val msg = backStackEntry.arguments?.getString("message") ?: ""
            val rec = backStackEntry.arguments?.getString("recipient") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val col = backStackEntry.arguments?.getLong("color") ?: 0xFFFFFFFF

            // And shows the full screen letter
            FocusLetterScreen(
                message = msg,
                recipient = rec,
                date = date,
                colorHex = col,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.VERIFY) {
            VerificationScreen(
                onVerificationSuccess = {
                    val userEmail = supabase.auth.currentUserOrNull()?.email ?: ""
                    val domain = userEmail.substringAfter("@")
                    navController.navigate("feed/$domain") {
                        popUpTo(Routes.CHOOSE_SPACE)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FEED_WITH_ARG,
            arguments = listOf(navArgument("spaceName") { type = NavType.StringType })
        ) { backStackEntry ->
            val spaceName = backStackEntry.arguments?.getString("spaceName") ?: "Global"
            var letters by remember { mutableStateOf<List<Letter>>(emptyList()) }
            var isLoading by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            val refreshLetters = {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        letters = supabase.from("letters")
                            .select {
                                filter { eq("space", spaceName) }
                                order("created_at", Order.DESCENDING)
                            }.decodeList()
                    } catch (e: Exception) {
                        println("Error: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(spaceName) { refreshLetters() }

            FeedScreen(
                spaceName = spaceName,
                letters = letters,
                isLoading = isLoading,
                onHeaderClick = { navController.popBackStack() },
                onWriteClick = { navController.navigate("${Routes.WRITE}/$spaceName") },
                onRefresh = { refreshLetters() },

                onLetterClick = { letter ->
                    val encodedMsg = android.net.Uri.encode(letter.message)
                    val encodedRec = android.net.Uri.encode(letter.recipient)
                    val encodedDate = android.net.Uri.encode(getTimeAgo(letter.createdAt))
                    navController.navigate("focus/$encodedMsg/$encodedRec/$encodedDate/${letter.colorHex}")
                }
            )
        }

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
    onRefresh: () -> Unit,
    onLetterClick: (Letter) -> Unit
) {
    Scaffold(
        containerColor = PaperWhite,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onWriteClick() },
                containerColor = InkCharcoal,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Write")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(1f),
                    color = InkCharcoal
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onHeaderClick() }
                                .padding(8.dp)
                                .offset(x = (-8).dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = spaceName,
                                fontFamily = InterFont,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkCharcoal
                            )
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = InkCharcoal)
                        }
                    }
                }

                // This is the clean loop. Copy ONLY this.
                items(letters) { letter ->
                    LetterCard(letter) {
                        onLetterClick(letter)
                    }
                }
                }
            }

            if (letters.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No letters yet.", fontFamily = InterFont, color = Color.Gray)
                }
            }
        }
    }


@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        // FORCE LOGOUT ON STARTUP
        // This simulates "Logout on Close" because every time the app starts fresh,
        // we kill the previous session.
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            // Ignore error if already signed out
        }

        delay(2000)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize().background(PaperWhite), contentAlignment = Alignment.Center) {
        Text("Unsaid.", color = InkCharcoal, fontSize = 48.sp, fontFamily = LibreFont, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
    }
}

@Composable
fun WelcomeScreen(onReadClick: () -> Unit, onWriteClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(PaperWhite).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Unsaid.",
                fontFamily = LibreFont,
                fontSize = 48.sp,
                color = InkCharcoal,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // UPDATED SUBTITLE
            Text(
                "For the words you never said out loud.",
                fontFamily = InterFont,
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 64.dp),
                textAlign = TextAlign.Center
            )

            // BUTTONS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(0.1f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkCharcoal)
                    .bounceClick(onClick = onReadClick),
                contentAlignment = Alignment.Center
            ) {
                Text("Read Letters", fontFamily = InterFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.5.dp, InkCharcoal, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick(onClick = onWriteClick),
                contentAlignment = Alignment.Center
            ) {
                Text("Write a Letter", fontFamily = InterFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkCharcoal)
            }
        }
        // Footer REMOVED
    }
}

@Composable
fun ChooseSpaceScreen(
    onGlobalClick: () -> Unit,
    onCollegeClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = PaperWhite,
        topBar = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 48.dp, start = 16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkCharcoal)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Choose Space",
                fontFamily = InterFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = InkCharcoal,
                modifier = Modifier.padding(bottom = 48.dp)
            )
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
    var otpToken by remember { mutableStateOf("") }
    var verificationState by remember { mutableStateOf(0) } // 0: Input, 1: Loading, 2: OTP
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // List of public domains to BLOCK
    val publicDomains = listOf(
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
        "live.com", "icloud.com", "aol.com", "protonmail.com"
    )

    Scaffold(
        containerColor = PaperWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(top = 48.dp, start = 16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkCharcoal)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "College/Work Verification",
                fontFamily = InterFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = InkCharcoal,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (verificationState == 2) {
                // STATE 2: OTP ENTRY
                Text(
                    "Check your inbox!",
                    fontFamily = InterFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkCharcoal
                )
                Text(
                    "Code sent to $email",
                    fontFamily = InterFont,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                TextField(
                    value = otpToken,
                    onValueChange = { otpToken = it },
                    placeholder = { Text("Enter 8-digit code", color = Color.LightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (otpToken.length == 8) {
                            scope.launch {
                                try {
                                    supabase.auth.verifyEmailOtp(
                                        type = OtpType.Email.EMAIL,
                                        email = email,
                                        token = otpToken
                                    )
                                    onVerificationSuccess()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Invalid code. Try again.")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        "Verify Code",
                        color = Color.White,
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // STATE 1: EMAIL ENTRY
                Text(
                    "Use your official work or college email. Public emails (Gmail, Yahoo, etc.) are not allowed.",
                    fontFamily = InterFont,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Text(
                    "Official Email",
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = InkCharcoal,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("student@college.edu", color = Color.LightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (email.isNotEmpty()) InkCharcoal else Color.LightGray)
                        .bounceClick {
                            if (email.isNotEmpty() && verificationState == 0) {
                                // --- SECURITY CHECK: Block Public Domains ---
                                val domain = email.substringAfter("@", "").lowercase()
                                if (publicDomains.contains(domain) || domain.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Public emails not allowed. Use work/college email."
                                        )
                                    }
                                    return@bounceClick
                                }

                                // Check passed: Send OTP
                                verificationState = 1
                                scope.launch {
                                    try {
                                        supabase.auth.signInWith(OTP) {
                                            this.email = email
                                        }
                                        verificationState = 2
                                    } catch(e: Exception) {
                                        verificationState = 0
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (verificationState == 1) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Send Verification Link",
                            fontFamily = InterFont,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumSelectionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.05f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(PaperWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = InkCharcoal)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = InkCharcoal
                )
                Text(
                    description,
                    fontFamily = InterFont,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(spaceName: String, onBackClick: () -> Unit, onPostClick: (Letter) -> Unit) {
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }

    // STATES
    var isSent by remember { mutableStateOf(false) }    // Success Screen
    var isBlocked by remember { mutableStateOf(false) } // Limit Screen
    var timeRemaining by remember { mutableStateOf("") }

    // LOCAL STORAGE (Limit Logic)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("UnsaidLimits", android.content.Context.MODE_PRIVATE) }

    // Check Limits on Load
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val firstPostTime = prefs.getLong("first_post_time", 0L)
        val count = prefs.getInt("daily_count", 0)

        if (now - firstPostTime > 24 * 60 * 60 * 1000) {
            // 24 hours passed, reset
            prefs.edit().putInt("daily_count", 0).putLong("first_post_time", 0L).apply()
        } else if (count >= 30) {
            // Limit reached
            isBlocked = true
            val millisLeft = (firstPostTime + 24 * 60 * 60 * 1000) - now
            val hours = millisLeft / (1000 * 60 * 60)
            timeRemaining = "$hours hours"
        }
    }

    // --- SCREEN 1: BLOCKED (Limit Reached) ---
    if (isBlocked) {
        Box(modifier = Modifier.fillMaxSize().background(PaperWhite), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = InkCharcoal, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Daily Limit Reached", fontFamily = LibreFont, fontSize = 24.sp, color = InkCharcoal)
                Text("You can write again in $timeRemaining.", fontFamily = InterFont, color = TextGray, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal)) {
                    Text("Back to Feed", color = Color.White)
                }
            }
        }
        return
    }

    // --- SCREEN 2: SUCCESS (Letter Posted) ---
    if (isSent) {
        Box(modifier = Modifier.fillMaxSize().background(PaperWhite), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text("Sent.", fontFamily = LibreFont, fontSize = 48.sp, color = InkCharcoal)
                Text("Your words are out there now.", fontFamily = InterFont, color = TextGray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

                // Option A: Go to specific feed
                Button(
                    onClick = onBackClick, // Navigates back to the feed we came from
                    colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View $spaceName Feed", fontFamily = InterFont, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option B: Go Home (requires logic passed from parent, but for now we just close)
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, InkCharcoal)
                ) {
                    Text("Close", fontFamily = InterFont, fontWeight = FontWeight.Bold, color = InkCharcoal)
                }
            }
        }
        return
    }

    // --- SCREEN 3: WRITE (Standard UI) ---
    val isDarkPaper = selectedColor == Color(0xFF1A1A1A)
    val textColor = if (isDarkPaper) Color.White else InkCharcoal
    val hintColor = if (isDarkPaper) Color(0xFFCCCCCC) else TextGray

    Scaffold(
        containerColor = PaperWhite,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkCharcoal) }
                Text(spaceName, fontFamily = InterFont, color = TextGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(PaperWhite).navigationBarsPadding().padding(24.dp)) {
                // Color Picker
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center) {
                    listOf(Color(0xFFFFFFFF), Color(0xFFEEF2F6), Color(0xFFF2E8D9), Color(0xFFE1E6E1), Color(0xFF1A1A1A)).forEach { color ->
                        Box(modifier = Modifier.padding(8.dp).size(36.dp).clip(CircleShape).background(color).border(1.dp, if (selectedColor == color) InkCharcoal else Color(0xFFE0E0E0), CircleShape).clickable { selectedColor = color })
                    }
                }
                // Post Button
                Button(
                    onClick = {
                        if (message.isNotEmpty()) {
                            // 1. UPDATE LIMITS
                            val now = System.currentTimeMillis()
                            val count = prefs.getInt("daily_count", 0)
                            if (count == 0) prefs.edit().putLong("first_post_time", now).apply()
                            prefs.edit().putInt("daily_count", count + 1).apply()

                            // 2. POST
                            onPostClick(Letter(recipient = recipient.ifEmpty { "Anonymous" }, message = message, space = spaceName, colorHex = selectedColor.toArgb().toLong()))

                            // 3. SHOW SUCCESS
                            isSent = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Post Letter", fontFamily = InterFont, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 400.dp).border(2.dp, if (selectedColor == Color(0xFF1A1A1A)) Color.Transparent else Color(0xFFD1D1D1), RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = selectedColor),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    TextField(value = recipient, onValueChange = { if (it.length <= 25) recipient = it }, placeholder = { Text("To: ...", color = hintColor.copy(0.5f)) }, textStyle = TextStyle(fontFamily = InterFont, fontSize = 14.sp, color = textColor), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth())
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(hintColor.copy(0.1f)).padding(vertical = 8.dp))
                    TextField(value = message, onValueChange = { if (it.length <= 500) message = it }, placeholder = { Text("Write what you never said...", color = hintColor.copy(0.5f)) }, textStyle = TextStyle(fontFamily = LibreFont, fontSize = 18.sp, lineHeight = 28.sp, color = textColor), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
@Composable
fun LetterCard(letter: Letter, onClick: () -> Unit) {
    // 1. GET THE COLOR
    val paperColor = Color(letter.colorHex)

    // 2. ROBUST DARK MODE CHECK
    // Instead of checking the ID, we check the 'red' value.
    // Dark colors (like Black/Midnight) have very low red values (< 0.5).
    // Light colors (White, Beige, Mint) have high red values (> 0.5).
    val isDark = paperColor.red < 0.5f

    // 3. SET COLORS (High Contrast)
    val textColor = if (isDark) Color.White else InkCharcoal // Force Pure White
    val metaColor = if (isDark) Color(0xFFCCCCCC) else TextGray
    val dividerColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
    val borderColor = if (isDark) Color(0xFF333333) else Color(0xFFD1D1D1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = paperColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("To: ${letter.recipient}", fontFamily = InterFont, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = metaColor)
                Text(getTimeAgo(letter.createdAt), fontFamily = InterFont, fontSize = 12.sp, color = metaColor.copy(alpha = 0.7f))
            }

            // DIVIDER
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
            Spacer(modifier = Modifier.height(16.dp))

            // BODY
            Text(
                text = letter.message,
                fontFamily = LibreFont,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                color = textColor // Guaranteed White
            )
        }
    }
}

@Composable
fun FocusLetterScreen(
    message: String,
    recipient: String,
    date: String,
    colorHex: Long,
    onBackClick: () -> Unit
) {
    // 1. GET THE COLOR
    val paperColor = Color(colorHex)

    // 2. ROBUST DARK MODE CHECK
    val isDark = paperColor.red < 0.5f

    // 3. SET COLORS
    val textColor = if (isDark) Color.White else InkCharcoal // Force Pure White
    val metaColor = if (isDark) Color(0xFFCCCCCC) else TextGray.copy(alpha = 0.6f)
    val borderColor = if (isDark) Color(0xFF333333) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable { onBackClick() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = paperColor),
            elevation = CardDefaults.cardElevation(20.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                // Header
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("To: $recipient", fontFamily = InterFont, fontSize = 16.sp, color = metaColor)
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = metaColor, modifier = Modifier.clickable { onBackClick() })
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Message (High Contrast)
                Text(
                    text = message,
                    fontFamily = LibreFont,
                    fontSize = 24.sp,
                    lineHeight = 36.sp,
                    color = textColor // Guaranteed White
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Date
                Text(date.uppercase(), fontFamily = InterFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = metaColor, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}     // The "Paper" Card for Writing


