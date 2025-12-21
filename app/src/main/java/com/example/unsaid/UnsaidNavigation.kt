package com.example.unsaid

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.luminance // To check for dark colors automatically
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
// --- NEW PALETTE ---
val AppPalette = listOf(
    Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFD1D1D1), Color(0xFF333333), Color(0xFFFDD835), Color(0xFFFBC02D),
    Color(0xFFBCAAA4), Color(0xFF795548), Color(0xFFFFF9C4), Color(0xFFFFE082), Color(0xFFF8BBD0), Color(0xFFF48FB1),
    Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFF81C784), Color(0xFFAED581),
    Color(0xFFCDDC39), Color(0xFFDCEDC8), Color(0xFF5C6BC0), Color(0xFF3949AB), Color(0xFF7986CB), Color(0xFF3F51B5),
    Color(0xFFD32F2F), Color(0xFFC62828), Color(0xFFD84315), Color(0xFFEF6C00), Color(0xFF4DB6AC), Color(0xFF66BB6A),
    Color(0xFF558B2F), Color(0xFF2E7D32), Color(0xFF9575CD), Color(0xFFAD1457), Color(0xFF673AB7), Color(0xFFFFF59D)
)


val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL, // <--- Reads from hidden file
    supabaseKey = BuildConfig.SUPABASE_KEY
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
    @SerialName("created_at") val createdAt: String? = null,
    val reports: Int = 0,
    @SerialName("is_hidden") val isHidden: Boolean = false
)


// --- ROUTES ---
object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val CHOOSE_SPACE = "choose_space"


    const val FOCUS = "focus/{id}/{message}/{recipient}/{date}/{color}/{reports}"

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
                navArgument("id") { type = NavType.LongType }, // <--- NEW
                navArgument("message") { type = NavType.StringType },
                navArgument("recipient") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("color") { type = NavType.LongType },
                navArgument("reports") { type = NavType.IntType } // <--- NEW
            )
        ) { backStackEntry ->
            // Extract the new data
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val msg = backStackEntry.arguments?.getString("message") ?: ""
            val rec = backStackEntry.arguments?.getString("recipient") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val col = backStackEntry.arguments?.getLong("color") ?: 0xFFFFFFFF
            val rep = backStackEntry.arguments?.getInt("reports") ?: 0

            // Pass it to the screen (Fixes the Red Error)
            FocusLetterScreen(
                letterId = id,
                message = msg,
                recipient = rec,
                date = date,
                colorHex = col,
                currentReports = rep,
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
                                filter { eq("space", spaceName)
                                    eq("is_hidden", false)
                                }
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
                    // We now pass ID and Reports in the URL
                    navController.navigate("focus/${letter.id}/$encodedMsg/$encodedRec/$encodedDate/${letter.colorHex}/${letter.reports}")
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
    // --- SEARCH STATES ---
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // --- FILTER LOGIC (RECIPIENT ONLY) ---
    val filteredLetters = remember(searchQuery, letters) {
        if (searchQuery.isEmpty()) letters else letters.filter {
            it.recipient.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onWriteClick() },
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Write")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // --- HEADER AREA ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp), // Removed .height(48.dp) to prevent cutting text
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isSearchActive) {
                        // --- SEARCH MODE ---
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search To: Name...", fontFamily = InterFont, fontSize = 16.sp) }, // Increased font size slightly
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = TextStyle(fontFamily = InterFont, fontSize = 16.sp, color = Color.Black),
                            modifier = Modifier
                                .weight(1f)
                                .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp)), // Clip prevents background spill
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                                }
                            }
                        )
                    } else {
                        // --- NORMAL TITLE MODE ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onHeaderClick() }
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = spaceName,
                                fontFamily = InterFont,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Row {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
                            }
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Black)
                            }
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color.Black)
                }

                // --- MASONRY GRID ---
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredLetters) { letter ->
                        LetterCard(
                            recipient = letter.recipient,
                            message = letter.message,
                            colorHex = letter.colorHex,
                            onClick = { onLetterClick(letter) }
                        )
                    }
                }

                // Empty State
                if (filteredLetters.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (searchQuery.isNotEmpty()) "No one found with that name." else "No letters yet.",
                            fontFamily = InterFont,
                            color = Color.Gray
                        )
                    }
                }
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onReadClick: () -> Unit, onWriteClick: () -> Unit) {
    // 1. GET STORAGE
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("unsaid_prefs", android.content.Context.MODE_PRIVATE) }

    // 2. CHECK IF SEEN BEFORE
    // If "onboarding_complete" is true, showOnboarding becomes false.
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_complete", false)) }

    Box(modifier = Modifier.fillMaxSize().background(PaperWhite)) {

        // --- 1. THE MAIN MENU (Visible if Onboarding is Done) ---
        AnimatedVisibility(
            visible = !showOnboarding,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaperWhite),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .offset(y = (-40).dp)
                ) {
                    Text(
                        "Unsaid.",
                        fontFamily = LibreFont,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = InkCharcoal,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "What's on your mind?",
                        fontFamily = InterFont,
                        fontSize = 18.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 48.dp)
                    )

                    // READ CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .graphicsLayer { rotationZ = -4f }
                            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.3f))
                            .background(Color(0xFF4DB6AC), RoundedCornerShape(16.dp))
                            .border(3.dp, InkCharcoal, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClick(onClick = onReadClick)
                            .padding(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email, // Fixed Icon
                                contentDescription = null,
                                tint = InkCharcoal,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "READ what the world has to say...",
                                fontFamily = LibreFont,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkCharcoal,
                                lineHeight = 30.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height((-20).dp))

                    // WRITE CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(160.dp)
                            .graphicsLayer { rotationZ = 3f }
                            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.3f))
                            .background(Color(0xFFFFE082), RoundedCornerShape(16.dp))
                            .border(3.dp, InkCharcoal, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClick(onClick = onWriteClick)
                            .padding(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = InkCharcoal,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Drop a letter, share your UNSAID...",
                                fontFamily = LibreFont,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkCharcoal,
                                lineHeight = 30.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 2. THE ONBOARDING OVERLAY (Visible only FIRST TIME) ---
        AnimatedVisibility(
            visible = showOnboarding,
            enter = fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 3 })
            val coroutineScope = rememberCoroutineScope()

            Column(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Dynamic Card Visual
                        val rotation = when(page) { 0 -> -5f; 1 -> 5f; else -> 0f }
                        val color = when(page) { 0 -> AppPalette[24]; 1 -> AppPalette[29]; else -> AppPalette[20] }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(0.8f)
                                .graphicsLayer { rotationZ = rotation }
                                .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.2f))
                                .background(color, RoundedCornerShape(16.dp))
                                .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            // Fake Card Content
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Box(modifier = Modifier.width(60.dp).height(8.dp).background(Color.Black.copy(0.1f), CircleShape))
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black.copy(0.2f))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Black.copy(0.1f), CircleShape))
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.8f).height(8.dp).background(Color.Black.copy(0.1f), CircleShape))
                                Spacer(modifier = Modifier.weight(1f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Box(modifier = Modifier.width(30.dp).height(6.dp).background(Color.Black.copy(0.1f), CircleShape))
                                    Box(modifier = Modifier.width(30.dp).height(6.dp).background(Color.Black.copy(0.1f), CircleShape))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        val title = when(page) {
                            0 -> "Everyone has something they never said."
                            1 -> "Here, you can say it without being judged."
                            else -> "Write anonymously. Share with the world."
                        }
                        val subtitle = when(page) {
                            0 -> "A secret, a confession, or just a thought."
                            1 -> "No names. No profiles. Just raw words."
                            else -> "Or confess specifically within your college."
                        }

                        Text(
                            text = title,
                            fontFamily = LibreFont,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp,
                            color = InkCharcoal
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = subtitle,
                            fontFamily = LibreFont,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = InkCharcoal,
                            lineHeight = 30.sp
                        )
                    }
                }

                // Footer Navigation
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(if (isSelected) 24.dp else 8.dp)
                            val color = if (isSelected) InkCharcoal else Color.LightGray
                            Box(modifier = Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
                        }
                    }

                    // Next / Get Started Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(InkCharcoal)
                            .clickable {
                                coroutineScope.launch {
                                    if (pagerState.currentPage < 2) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else {
                                        // 3. SAVE TO STORAGE & CLOSE
                                        prefs.edit().putBoolean("onboarding_complete", true).apply()
                                        showOnboarding = false
                                    }
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == 2) "Get Started" else "Next",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFont
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ChooseSpaceScreen(
    onGlobalClick: () -> Unit,
    onCollegeClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFFAFAFA), // Paper White
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // --- HEADER ---
            Text(
                "Choose a Space",
                fontFamily = LibreFont,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = InkCharcoal
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "A space to speak freely, completely anonymous.",
                fontFamily = InterFont,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- GLOBAL CARD ---
            PremiumSelectionCard(
                title = "Global",
                description = "Letter archive for the world.",
                icon = Icons.Default.Home, // Represents the "Home" of the internet
                onClick = onGlobalClick,
                color = Color(0xFFE3F2FD) // Soft Blue Circle
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- COLLEGE CARD ---
            PremiumSelectionCard(
                title = "College",
                description = "Anonymous confessions, exclusively for your college.",
                icon = Icons.Default.Person, // Represents "You/Your Community"
                onClick = onCollegeClick,
                color = Color(0xFFE8F5E9) // Soft Green Circle
            )
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
        containerColor = Color(0xFFFAFAFA), // Paper White
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- HEADER ---
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock, // Lock Icon
                    contentDescription = null,
                    tint = InkCharcoal,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Unlock Your Campus",
                fontFamily = LibreFont, // Premium Serif
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = InkCharcoal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "To ensure this space remains exclusive to students, we need to verify you belong here.",
                fontFamily = InterFont,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- THE PRIVACY PLEDGE CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Soft Green
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "100% Anonymous",
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Your email is used ONLY for verification. It is never displayed, shared, or linked to your letters.",
                            fontFamily = InterFont,
                            fontSize = 13.sp,
                            color = Color(0xFF1B5E20),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- INPUT FIELDS ---
            if (verificationState == 2) {
                // STATE 2: OTP
                Text("Check your inbox for the code!", fontFamily = InterFont, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = otpToken,
                    onValueChange = { otpToken = it },
                    placeholder = { Text("Enter 6-digit code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().border(2.dp, InkCharcoal, RoundedCornerShape(12.dp)), // Retro Border
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (otpToken.length >= 6) {
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
                    Text("Verify & Enter", color = Color.White, fontWeight = FontWeight.Bold)
                }

            } else {
                // STATE 1: EMAIL
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
                    placeholder = { Text("student@srmist.edu.in", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().border(2.dp, InkCharcoal, RoundedCornerShape(12.dp)), // Retro Border
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isNotEmpty() && verificationState == 0) {
                            // BLOCK PUBLIC DOMAINS
                            val domain = email.substringAfter("@", "").lowercase()
                            if (publicDomains.contains(domain) || domain.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("Please use your college email (not Gmail).") }
                                return@Button
                            }

                            // SEND OTP
                            verificationState = 1
                            scope.launch {
                                try {
                                    supabase.auth.signInWith(OTP) { this.email = email }
                                    verificationState = 2
                                } catch(e: Exception) {
                                    verificationState = 0
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (verificationState == 1) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Send Secure Link", color = Color.White, fontWeight = FontWeight.Bold)
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
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // RETRO STYLE: Thick Border + Shadow
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.1f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, InkCharcoal, RoundedCornerShape(16.dp)) // <--- The Retro Border
            .bounceClick(onClick = onClick)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = InkCharcoal,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Text Content
            Column {
                Text(
                    text = title,
                    fontFamily = LibreFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkCharcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontFamily = InterFont,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(spaceName: String, onBackClick: () -> Unit, onPostClick: (Letter) -> Unit) {
    // 1. INPUT STATES
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(AppPalette[4]) }

    // 2. SAFETY STATES
    var isChecked by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(spaceName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        },
        bottomBar = {
            // CHECKBOX + BUTTON (Unchanged)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { isChecked = !isChecked },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color.Black)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("I agree to the Community Guidelines", fontFamily = InterFont, fontSize = 12.sp, color = Color.Black)
                        Text("Read Rules", fontFamily = InterFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Blue, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, modifier = Modifier.clickable { showGuidelines = true })
                    }
                }
                Button(
                    onClick = {
                        if (message.isNotEmpty() && isChecked) {
                            onPostClick(Letter(
                                recipient = recipient.ifEmpty { "Anonymous" },
                                message = message,
                                space = spaceName,
                                colorHex = selectedColor.toArgb().toLong(),
                                reports = 0,
                                isHidden = false
                            ))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isChecked) Color.Black else Color.LightGray),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Submit Your Message", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Choose a Color", fontFamily = LibreFont, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))

            // --- COMPACT COLOR GRID ---
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 40.dp),
                modifier = Modifier.height(190.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(AppPalette) { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(
                                width = if (selectedColor == color) 2.dp else 1.dp,
                                color = if (selectedColor == color) Color.Black else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- EXPANDED LETTER CARD ---
            val paperColor = selectedColor
            val isDark = paperColor.luminance() < 0.5f
            val inkColor = if (isDark) Color.White else Color.Black
            val borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = paperColor)
            ) {
                Column {
                    // Header
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("To: ", fontFamily = LibreFont, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = inkColor)
                        TextField(
                            value = recipient,
                            onValueChange = { if (it.length <= 25) recipient = it },
                            placeholder = { Text("Enter Name", color = inkColor.copy(0.5f), fontFamily = LibreFont) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = inkColor, focusedTextColor = inkColor, unfocusedTextColor = inkColor
                            ),
                            textStyle = TextStyle(fontSize = 18.sp, fontFamily = LibreFont)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

                    // Body - WITH LIMIT & COUNTER
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = message,
                            onValueChange = {
                                if (it.length <= 150) message = it // <--- LIMIT SET TO 150
                            },
                            placeholder = { Text("Type Your Message Here...", color = inkColor.copy(0.5f), fontFamily = LibreFont) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = inkColor, focusedTextColor = inkColor, unfocusedTextColor = inkColor
                            ),
                            textStyle = TextStyle(fontSize = 20.sp, fontFamily = LibreFont, lineHeight = 30.sp)
                        )

                        // CHARACTER COUNTER
                        Text(
                            text = "${message.length} / 150",
                            fontFamily = InterFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkColor.copy(0.5f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

                    // Footer
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SEND", fontSize = 10.sp, fontWeight = FontWeight.Black, color = inkColor)
                        Text("#unsaid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = inkColor.copy(0.6f))
                        Text("BACK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = inkColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Popup logic (Unchanged)
    if (showGuidelines) {
        AlertDialog(
            onDismissRequest = { showGuidelines = false },
            containerColor = Color.White,
            title = { Text("Community Guidelines", fontFamily = InterFont, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("1. No Bullying: Don't attack specific people.", fontSize = 14.sp, fontFamily = InterFont)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("2. No Hate Speech: Zero tolerance for racism/sexism.", fontSize = 14.sp, fontFamily = InterFont)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("3. No Doxing: Do not share private phone numbers.", fontSize = 14.sp, fontFamily = InterFont)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("4. Safety: Threats of violence will be reported.", fontSize = 14.sp, fontFamily = InterFont)
                }
            },
            confirmButton = {
                Button(onClick = { showGuidelines = false; isChecked = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                    Text("I Understand")
                }
            }
        )
    }
}
@Composable
fun LetterCard(
    recipient: String,
    message: String,
    colorHex: Long,
    onClick: () -> Unit = {}
) {
    val paperColor = Color(colorHex)
    // Auto-detect text color based on brightness (Luminance)
    // If background is dark (< 0.5), text is White. Else, Black.
    val isDark = paperColor.luminance() < 0.5f
    val inkColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp) // Spacing between grid items
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)) // THICK BLACK BORDER
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = paperColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "To: $recipient",
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = inkColor,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.Email, // Envelope Icon
                    contentDescription = null,
                    tint = inkColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            // DIVIDER LINE
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

            // --- BODY ---
            Text(
                text = message,
                fontFamily = InterFont, // Or LibreFont if you prefer serif
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = inkColor,
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                minLines = 3 // Ensures cards have some height even if empty
            )

            // DIVIDER LINE
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

            // --- FOOTER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SEND", fontSize = 10.sp, fontWeight = FontWeight.Black, color = inkColor)
                Text("#unsaid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = inkColor.copy(0.6f))
                Text("BACK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = inkColor)
            }
        }
    }
}

@Composable
fun FocusLetterScreen(
    letterId: Long, // <--- CHANGED: We need ID to report
    message: String,
    recipient: String,
    date: String,
    colorHex: Long,
    currentReports: Int, // <--- NEW: Need current count
    onBackClick: () -> Unit
) {
    // 1. COLORS
    val paperColor = Color(colorHex)
    val isDark = paperColor.luminance() < 0.5f
    val inkColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black

    // 2. REPORT STATE
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    // Check if this device already reported this specific letter
    val prefs = remember { context.getSharedPreferences("reported_letters", android.content.Context.MODE_PRIVATE) }
    var hasReported by remember { mutableStateOf(prefs.getBoolean("reported_$letterId", false)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onBackClick() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = paperColor),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("To: $recipient", fontFamily = InterFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = inkColor)
                    Icon(Icons.Default.Email, contentDescription = null, tint = inkColor, modifier = Modifier.size(20.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

                // BODY
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(message, fontFamily = LibreFont, fontSize = 22.sp, lineHeight = 32.sp, color = inkColor)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(date.uppercase(), fontFamily = InterFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inkColor.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.End))
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

                // FOOTER (With Report Button)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // REPORT BUTTON
                    // REPORT BUTTON (Fixed Visibility)
                    Text(
                        text = if (hasReported) "REPORTED" else "REPORT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        // FIX: Use 'inkColor' so it matches the high-contrast theme (White/Black)
                        // If reported, we dim it slightly.
                        color = if (hasReported) inkColor.copy(alpha = 0.5f) else inkColor,
                        modifier = Modifier.clickable {
                            if (!hasReported) {
                                hasReported = true
                                prefs.edit().putBoolean("reported_$letterId", true).apply()

                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val newCount = currentReports + 1
                                        val shouldHide = newCount >= 5
                                        // Update Supabase
                                        supabase.from("letters").update({
                                            set("reports", newCount)
                                            set("is_hidden", shouldHide)
                                        }) {
                                            filter { eq("id", letterId) }
                                        }
                                    } catch (e: Exception) {
                                        println("Report error: ${e.message}")
                                    }
                                }
                            }
                        }
                    )

                    Text("#unsaid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inkColor.copy(0.6f))

                    Text("BACK", fontSize = 12.sp, fontWeight = FontWeight.Black, color = inkColor, modifier = Modifier.clickable { onBackClick() })
                }
            }
        }
    }
}


