package com.abhishek.unsaid

// --- ANDROID & COMPOSE IMPORTS ---
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// --- SUPABASE & NETWORK IMPORTS ---
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// --- PROJECT IMPORTS ---
import com.abhishek.unsaid.ui.theme.InkCharcoal
import com.abhishek.unsaid.ui.theme.InterFont
import com.abhishek.unsaid.ui.theme.LibreFont
import com.abhishek.unsaid.ui.theme.PaperWhite

/**
 * UNSAID - Anonymous Social Network
 *
 * This file contains the main Navigation Graph and UI logic for the application.
 * Architecture Note: For a production-grade app, logic currently inside Composables
 * (Supabase calls) should be moved to ViewModels to follow MVVM patterns.
 * Kept here for simplicity in this specific module.
 */

// --- CONSTANTS & CONFIGURATION ---
const val DAILY_POST_LIMIT = 3
const val MAX_MSG_LENGTH = 150
const val ANIMATION_DURATION = 1000

// --- THEME DEFINITIONS ---
val TextGray = Color(0xFF666666)

// Optimized Palette for visual distinctiveness
val AppPalette = listOf(
    Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFD1D1D1), Color(0xFF333333), Color(0xFFFDD835), Color(0xFFFBC02D),
    Color(0xFFBCAAA4), Color(0xFF795548), Color(0xFFFFF9C4), Color(0xFFFFE082), Color(0xFFF8BBD0), Color(0xFFF48FB1),
    Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFF81C784), Color(0xFFAED581),
    Color(0xFFCDDC39), Color(0xFFDCEDC8), Color(0xFF5C6BC0), Color(0xFF3949AB), Color(0xFF7986CB), Color(0xFF3F51B5),
    Color(0xFFD32F2F), Color(0xFFC62828), Color(0xFFD84315), Color(0xFFEF6C00), Color(0xFF4DB6AC), Color(0xFF66BB6A),
    Color(0xFF558B2F), Color(0xFF2E7D32), Color(0xFF9575CD), Color(0xFFAD1457), Color(0xFF673AB7), Color(0xFFFFF59D)
)

// Global Supabase Client (Ideally, inject this via Hilt/Koin)
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_KEY
) {
    install(Postgrest)
    install(Auth)
}

// --- DATA MODELS ---
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

@Serializable
data class VersionCheck(val min_version: Int)

// --- ROUTES ---
object Routes {
    const val SPLASH = "splash"
    const val TERMS = "terms"
    const val WELCOME = "welcome"
    const val CHOOSE_SPACE = "choose_space"
    const val FOCUS = "focus/{id}/{message}/{recipient}/{date}/{color}/{reports}"
    const val CHOOSE_WRITE_SPACE = "choose_write_space"
    const val VERIFY = "verify"
    const val WRITE = "write"
    const val FEED_WITH_ARG = "feed/{spaceName}"
}

// --- CUSTOM MODIFIERS ---

/**
 * Adds a bouncy spring animation to clicks.
 * Note: Uses [composed] for stateful modifier behavior.
 */
fun Modifier.bounceClick(scaleDown: Float = 0.95f, onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "BounceAnimation"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
    
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

// --- UTILS ---

fun getTimeAgo(isoString: String?): String {
    if (isoString == null) return "Just now"
    return try {
        val instant = java.time.Instant.parse(isoString)
        val now = java.time.Instant.now()
        val diff = java.time.Duration.between(instant, now)
        when {
            diff.toMinutes() < 1 -> "Just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
            diff.toHours() < 24 -> "${diff.toHours()}h ago"
            else -> "${diff.toDays()}d ago"
        }
    } catch (e: Exception) {
        "Recently"
    }
}

// ==========================================
// MAIN NAVIGATION GRAPH
// ==========================================

@Composable
fun UnsaidNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        
        // Splash & Version Check
        composable(Routes.SPLASH) {
            SplashScreen {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }
        
        composable(Routes.TERMS) {
            TermsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onReadClick = { navController.navigate(Routes.CHOOSE_SPACE) },
                onWriteClick = { navController.navigate(Routes.CHOOSE_WRITE_SPACE) }
            )
        }

        // Choosing where to read
        composable(Routes.CHOOSE_SPACE) {
            ChooseSpaceScreen(
                onGlobalClick = { navController.navigate("feed/Global") },
                onCollegeClick = {
                    // Check Session State
                    val currentUser = supabase.auth.currentUserOrNull()
                    if (currentUser != null) {
                        val domain = currentUser.email?.substringAfter("@") ?: "college"
                        navController.navigate("feed/$domain")
                    } else {
                        navController.navigate(Routes.VERIFY)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Choosing where to write
        composable(Routes.CHOOSE_WRITE_SPACE) {
            ChooseSpaceScreen(
                onGlobalClick = { navController.navigate("${Routes.WRITE}/Global") },
                onCollegeClick = {
                    val currentUser = supabase.auth.currentUserOrNull()
                    if (currentUser != null) {
                        val domain = currentUser.email?.substringAfter("@") ?: "college"
                        navController.navigate("${Routes.WRITE}/$domain")
                    } else {
                        navController.navigate(Routes.VERIFY)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Detailed View of a Letter
        composable(
            route = Routes.FOCUS,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("message") { type = NavType.StringType },
                navArgument("recipient") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("color") { type = NavType.LongType },
                navArgument("reports") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            FocusLetterScreen(
                letterId = backStackEntry.arguments?.getLong("id") ?: 0L,
                message = backStackEntry.arguments?.getString("message") ?: "",
                recipient = backStackEntry.arguments?.getString("recipient") ?: "",
                date = backStackEntry.arguments?.getString("date") ?: "",
                colorHex = backStackEntry.arguments?.getLong("color") ?: 0xFFFFFFFF,
                currentReports = backStackEntry.arguments?.getInt("reports") ?: 0,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Auth Flow
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

        // Main Feed
        composable(
            route = Routes.FEED_WITH_ARG,
            arguments = listOf(navArgument("spaceName") { type = NavType.StringType })
        ) { backStackEntry ->
            val spaceName = backStackEntry.arguments?.getString("spaceName") ?: "Global"
            var letters by remember { mutableStateOf<List<Letter>>(emptyList()) }
            var isLoading by remember { mutableStateOf(false) }

            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            // Logic to fetch letters
            // TODO: Move this logic to a ViewModel
            val refreshLetters = {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        // 1. Fetch from Supabase
                        val rawList = supabase.from("letters")
                            .select {
                                filter {
                                    eq("space", spaceName)
                                    eq("is_hidden", false)
                                }
                                order("created_at", Order.DESCENDING)
                                limit(20) // Pagination limit
                            }.decodeList<Letter>()

                        // 2. Local Filtering (Blocked Content)
                        val prefs = context.getSharedPreferences("reported_letters", Context.MODE_PRIVATE)
                        val cleanList = rawList.filter { letter ->
                            !prefs.getBoolean("reported_${letter.id}", false)
                        }

                        // 3. Update UI
                        letters = cleanList
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            }

            // Auto-refresh when user returns to this screen (e.g. after reporting a post)
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        refreshLetters()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            FeedScreen(
                spaceName = spaceName,
                letters = letters,
                isLoading = isLoading,
                onHeaderClick = { navController.popBackStack() },
                onWriteClick = { navController.navigate("${Routes.WRITE}/$spaceName") },
                onRefresh = { refreshLetters() },
                onLetterClick = { letter ->
                    val encodedMsg = Uri.encode(letter.message)
                    val encodedRec = Uri.encode(letter.recipient)
                    val encodedDate = Uri.encode(getTimeAgo(letter.createdAt))
                    navController.navigate("focus/${letter.id}/$encodedMsg/$encodedRec/$encodedDate/${letter.colorHex}/${letter.reports}")
                }
            )
        }

        // Write Screen
        composable(
            route = "${Routes.WRITE}/{spaceName}",
            arguments = listOf(navArgument("spaceName") { type = NavType.StringType })
        ) { backStackEntry ->
            val spaceName = backStackEntry.arguments?.getString("spaceName") ?: "Global"
            WriteScreen(
                spaceName = spaceName,
                onNavigateBack = { navController.popBackStack() },
                onTermsClick = { navController.navigate(Routes.TERMS) }
            )
        }
    }
}

// ==========================================
// SCREEN COMPOSABLES
// ==========================================

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
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Efficiently filter list when query changes
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
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search To: Name...", fontFamily = InterFont, fontSize = 16.sp) },
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
                                .clip(RoundedCornerShape(8.dp)),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
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

                // Masonry Grid for aesthetic layout
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

                // Empty State Handling
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onReadClick: () -> Unit, onWriteClick: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("unsaid_prefs", Context.MODE_PRIVATE) }
    
    // Onboarding State: Defaults to TRUE unless explicitly saved as false
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_complete", false)) }

    Box(modifier = Modifier.fillMaxSize().background(PaperWhite)) {

        // --- MAIN MENU (Visible after onboarding) ---
        AnimatedVisibility(
            visible = !showOnboarding,
            enter = fadeIn(animationSpec = tween(ANIMATION_DURATION)),
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
                                imageVector = Icons.Default.Email,
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

        // --- ONBOARDING OVERLAY ---
        AnimatedVisibility(
            visible = showOnboarding,
            enter = fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            val pagerState = rememberPagerState(pageCount = { 3 })
            val coroutineScope = rememberCoroutineScope()

            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Dynamic Card Visual Animation
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
                            // Mock Card Content
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Box(modifier = Modifier.width(60.dp).height(8.dp).background(Color.Black.copy(0.1f), CircleShape))
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black.copy(0.2f))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Black.copy(0.1f), CircleShape))
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.8f).height(8.dp).background(Color.Black.copy(0.1f), CircleShape))
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

                        Text(title, fontFamily = LibreFont, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 36.sp, color = InkCharcoal)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(subtitle, fontFamily = LibreFont, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = InkCharcoal, lineHeight = 30.sp)
                    }
                }

                // Footer Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "dotWidth")
                            val color = if (isSelected) InkCharcoal else Color.LightGray
                            Box(modifier = Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(InkCharcoal)
                            .clickable {
                                coroutineScope.launch {
                                    if (pagerState.currentPage < 2) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else {
                                        prefs.edit().putBoolean("onboarding_complete", true).apply()
                                        showOnboarding = false
                                    }
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(if (pagerState.currentPage == 2) "Get Started" else "Next", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InterFont)
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
        containerColor = Color(0xFFFAFAFA),
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
            Text("Choose a Space", fontFamily = LibreFont, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = InkCharcoal)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Where do you want to speak?", fontFamily = InterFont, fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))

            ColorfulSpaceCard(
                title = "Global Feed",
                description = "The world's archive. Read letters from everywhere.",
                icon = Icons.Default.Home,
                color = Color(0xFFFFE082),
                rotation = -3f,
                onClick = onGlobalClick
            )
            Spacer(modifier = Modifier.height(32.dp))
            ColorfulSpaceCard(
                title = "College Feed",
                description = "Anonymous confessions. Exclusive to your college.",
                icon = Icons.Default.Person,
                color = Color(0xFF81D4FA),
                rotation = 3f,
                onClick = onCollegeClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(onVerificationSuccess: () -> Unit, onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var otpToken by remember { mutableStateOf("") }
    var currentStep by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val publicDomains = listOf("gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "live.com", "icloud.com")

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
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
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = InkCharcoal, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Unlock Your Campus", fontFamily = LibreFont, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = InkCharcoal, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text("To ensure this space remains exclusive to students, we need to verify you belong here.", fontFamily = InterFont, fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(32.dp))

            // Info Card
            if (currentStep == 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("100% Anonymous", fontFamily = InterFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Your email is used ONLY for verification. It is never displayed.", fontFamily = InterFont, fontSize = 13.sp, color = Color(0xFF1B5E20), lineHeight = 18.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (currentStep == 1) {
                // STEP 2: OTP INPUT
                Text("Check your inbox for the code!", fontFamily = InterFont, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("(Check spam folder if needed)", fontFamily = InterFont, fontSize = 12.sp, color = Color.Red.copy(0.7f))
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
                    modifier = Modifier.fillMaxWidth().border(2.dp, InkCharcoal, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (otpToken.isNotEmpty()) {
                            isLoading = true
                            scope.launch {
                                try {
                                    supabase.auth.verifyEmailOtp(
                                        type = OtpType.Email.EMAIL,
                                        email = email.trim().lowercase(),
                                        token = otpToken
                                    )
                                    onVerificationSuccess()
                                } catch (e: Exception) {
                                    isLoading = false
                                    snackbarHostState.showSnackbar("Invalid code: ${e.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Verify & Enter", color = Color.White, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { currentStep = 0; isLoading = false }) {
                    Text("Change Email", color = Color.Gray, fontFamily = InterFont)
                }

            } else {
                // STEP 1: EMAIL INPUT
                Text("Official Email", fontFamily = InterFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkCharcoal, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("student@university.edu", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().border(2.dp, InkCharcoal, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val cleanEmail = email.trim().lowercase()
                        if (cleanEmail.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("Please enter your email") }
                            return@Button
                        }
                        
                        // QA/Reviewer Bypass - Remove in Production
                        if (cleanEmail.contains("demo")) {
                            isLoading = true
                            scope.launch {
                                try {
                                    supabase.auth.signInWith(Email) {
                                        this.email = "demo@srmist.edu.in"
                                        password = "demo1234"
                                    }
                                    onVerificationSuccess()
                                } catch (e: Exception) {
                                    isLoading = false
                                    snackbarHostState.showSnackbar("Backdoor Failed: ${e.message}")
                                }
                            }
                            return@Button
                        }

                        // Domain Validation
                        val domain = cleanEmail.substringAfter("@", "")
                        if (publicDomains.contains(domain) || domain.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("Please use your college email (not Gmail).") }
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            try {
                                supabase.auth.signInWith(OTP) { this.email = cleanEmail }
                                currentStep = 1
                                isLoading = false
                            } catch (e: Exception) {
                                isLoading = false
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Send Verification Code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    spaceName: String,
    onNavigateBack: () -> Unit,
    onTermsClick: () -> Unit
) {
    // Persistent Input States (Survives navigation)
    var recipient by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var isChecked by rememberSaveable { mutableStateOf(false) }
    var selectedColorInt by rememberSaveable { mutableIntStateOf(AppPalette[4].toArgb()) }
    
    val selectedColor = Color(selectedColorInt)
    
    // Transient States
    var isSending by remember { mutableStateOf(false) }
    var showThankYou by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("unsaid_prefs", Context.MODE_PRIVATE) }

    // Auto-navigate after success
    LaunchedEffect(showThankYou) {
        if (showThankYou) {
            delay(2000)
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFFAFAFA),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    Text(spaceName, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { isChecked = !isChecked },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isChecked, onCheckedChange = { isChecked = it }, colors = CheckboxDefaults.colors(checkedColor = Color.Black))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("I agree to the Community Guidelines", fontFamily = InterFont, fontSize = 12.sp, color = Color.Black)
                            Text("Read Terms & Policy", fontFamily = InterFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Blue, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { onTermsClick() })
                        }
                    }
                    Button(
                        onClick = {
                            if (message.isNotEmpty() && isChecked && !isSending) {
                                // Daily Limit Check
                                val today = java.time.LocalDate.now().toString()
                                val lastDate = prefs.getString("last_post_date", "")
                                var dailyCount = prefs.getInt("daily_post_count", 0)

                                if (lastDate != today) dailyCount = 0

                                if (dailyCount >= DAILY_POST_LIMIT) {
                                    scope.launch { snackbarHostState.showSnackbar("Daily limit reached ($DAILY_POST_LIMIT). Come back tomorrow!") }
                                    return@Button
                                }

                                isSending = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val newLetter = Letter(
                                            recipient = recipient.ifEmpty { "Anonymous" },
                                            message = message,
                                            space = spaceName,
                                            colorHex = selectedColorInt.toLong(),
                                            reports = 0,
                                            isHidden = false
                                        )
                                        supabase.from("letters").insert(newLetter)

                                        withContext(Dispatchers.Main) {
                                            prefs.edit().putString("last_post_date", today).putInt("daily_post_count", dailyCount + 1).apply()
                                            isSending = false
                                            showThankYou = true
                                        }
                                    } catch(e: Exception) {
                                        isSending = false
                                        scope.launch { snackbarHostState.showSnackbar("Error sending letter. Try again.") }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isChecked) Color.Black else Color.LightGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (isSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Submit Your Message", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues).fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
            ) {
                Text("Choose a Color", fontFamily = LibreFont, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally))

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
                                .border(width = if (selectedColor == color) 2.dp else 1.dp, color = if (selectedColor == color) Color.Black else Color.LightGray, shape = RoundedCornerShape(8.dp))
                                .clickable { selectedColorInt = color.toArgb() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val paperColor = selectedColor
                val isDark = paperColor.luminance() < 0.5f
                val inkColor = if (isDark) Color.White else Color.Black
                val borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black

                Card(
                    modifier = Modifier.fillMaxWidth().border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = paperColor)
                ) {
                    Column {
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

                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                value = message,
                                onValueChange = { if (it.length <= MAX_MSG_LENGTH) message = it },
                                placeholder = { Text("Type Your Message Here...", color = inkColor.copy(0.5f), fontFamily = LibreFont) },
                                modifier = Modifier.fillMaxWidth().height(350.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = inkColor, focusedTextColor = inkColor, unfocusedTextColor = inkColor
                                ),
                                textStyle = TextStyle(fontSize = 20.sp, fontFamily = LibreFont, lineHeight = 30.sp)
                            )
                            Text(
                                text = "${message.length} / $MAX_MSG_LENGTH",
                                fontFamily = InterFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkColor.copy(0.5f),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

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

        AnimatedVisibility(
            visible = showThankYou,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaperWhite.copy(alpha = 0.95f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = InkCharcoal,
                        modifier = Modifier.size(64.dp).border(3.dp, InkCharcoal, CircleShape).padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Unsaid.", fontFamily = LibreFont, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = InkCharcoal)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your letter has been sent.", fontFamily = InterFont, fontSize = 16.sp, color = Color.Gray)
                }
            }
        }
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
    val isDark = paperColor.luminance() < 0.5f
    val inkColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = paperColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "To: $recipient", fontFamily = InterFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = inkColor, maxLines = 1)
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = inkColor, modifier = Modifier.size(16.dp))
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))
            Text(text = message, fontFamily = InterFont, fontSize = 14.sp, lineHeight = 20.sp, color = inkColor, modifier = Modifier.padding(12.dp).fillMaxWidth(), minLines = 3)
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))
            Row(modifier = Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SEND", fontSize = 10.sp, fontWeight = FontWeight.Black, color = inkColor)
                Text("#unsaid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = inkColor.copy(0.6f))
                Text("BACK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = inkColor)
            }
        }
    }
}

@Composable
fun FocusLetterScreen(
    letterId: Long,
    message: String,
    recipient: String,
    date: String,
    colorHex: Long,
    currentReports: Int,
    onBackClick: () -> Unit
) {
    val paperColor = Color(colorHex)
    val isDark = paperColor.luminance() < 0.5f
    val inkColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.2f) else Color.Black

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("reported_letters", Context.MODE_PRIVATE) }
    var hasReported by remember { mutableStateOf(prefs.getBoolean("reported_$letterId", false)) }
    var isVisible by remember { mutableStateOf(true) }

    if (!isVisible) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Letter Reported.", color = Color.White, fontFamily = InterFont, fontWeight = FontWeight.Bold)
                Text("It has been hidden from your feed.", color = Color.Gray, fontSize = 12.sp, fontFamily = InterFont)
            }
            LaunchedEffect(Unit) {
                delay(1500)
                onBackClick()
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onBackClick() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).border(2.dp, Color.Black, RoundedCornerShape(12.dp)).clickable(enabled = false) {},
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = paperColor),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("To: $recipient", fontFamily = InterFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = inkColor)
                    Icon(Icons.Default.Email, contentDescription = null, tint = inkColor, modifier = Modifier.size(20.dp))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp).verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    Text(message, fontFamily = LibreFont, fontSize = 22.sp, lineHeight = 32.sp, color = inkColor)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(date.uppercase(), fontFamily = InterFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inkColor.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.End))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (hasReported) "REPORTED" else "REPORT",
                        fontSize = 12.sp, fontWeight = FontWeight.Black,
                        color = if (hasReported) inkColor.copy(alpha = 0.5f) else inkColor,
                        modifier = Modifier.clickable {
                            if (!hasReported) {
                                hasReported = true
                                isVisible = false
                                prefs.edit().putBoolean("reported_$letterId", true).apply()
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        supabase.postgrest.rpc("report_letter", buildJsonObject { put("row_id", letterId) })
                                    } catch (e: Exception) {
                                        e.printStackTrace()
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

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { supabase.auth.signOut() } catch (_: Exception) {}
        try {
            val result = supabase.from("app_version")
                .select(columns = Columns.list("min_version")) {
                    limit(1)
                    single()
                }.decodeAs<VersionCheck>()

            if (BuildConfig.VERSION_CODE < result.min_version) {
                showUpdateDialog = true
            } else {
                delay(2000)
                onTimeout()
            }
        } catch (e: Exception) {
            // Fail Open strategy for better UX if offline
            delay(2000)
            onTimeout()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PaperWhite), contentAlignment = Alignment.Center) {
        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Update Required", fontFamily = LibreFont, fontWeight = FontWeight.Bold) },
                text = { Text("This version of Unsaid is no longer supported. Please update to continue.", fontFamily = InterFont) },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Black) },
                confirmButton = {
                    Button(
                        onClick = {
                            val appPackageName = context.packageName
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                            } catch (e: android.content.ActivityNotFoundException) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal)
                    ) { Text("Update Now", color = Color.White) }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            Text("Unsaid.", color = InkCharcoal, fontSize = 48.sp, fontFamily = LibreFont, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", fontFamily = LibreFont, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("End User License Agreement (EULA)", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont)
            Spacer(modifier = Modifier.height(16.dp))
            LegalSection("1. Acceptance of Terms", "By accessing or using Unsaid, you agree to be bound by these Terms.")
            LegalSection("2. User Generated Content", "Unsaid allows you to post anonymous letters. You are solely responsible for the content you post.")
            LegalSection("3. Zero Tolerance Policy", "Harassment, hate speech, doxing, and sexually explicit content are strictly prohibited.")
            LegalSection("4. Content Moderation", "Reported content is hidden immediately and reviewed within 24 hours.")
            LegalSection("5. Disclaimer", "The Service is provided on an 'AS IS' basis.")
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = InkCharcoal),
                modifier = Modifier.fillMaxWidth()
            ) { Text("I Understand", color = Color.White) }
        }
    }
}

@Composable
fun LegalSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFont)
        Spacer(modifier = Modifier.height(4.dp))
        Text(body, fontSize = 13.sp, color = Color.Gray, fontFamily = InterFont, lineHeight = 20.sp)
    }
}

@Composable
fun ColorfulSpaceCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    rotation: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .graphicsLayer { rotationZ = rotation }
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.3f))
            .background(color, RoundedCornerShape(16.dp))
            .border(3.dp, InkCharcoal, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = InkCharcoal, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = title, fontFamily = LibreFont, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkCharcoal)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = description, fontFamily = InterFont, fontSize = 14.sp, color = InkCharcoal.copy(0.7f), lineHeight = 18.sp)
            }
        }
    }
}
