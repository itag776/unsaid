package com.abhishek.unsaid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhishek.unsaid.R

// 1. Define the Font Families
val InterFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.Bold)
)

val LibreFont = FontFamily(
    Font(R.font.libre_regular, FontWeight.Normal),
    Font(R.font.libre_bold, FontWeight.Bold)
)

// 2. Set the Styles
val Typography = Typography(
    // Headlines (The "Unsaid" Logo)
    headlineLarge = TextStyle(
        fontFamily = LibreFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        color = InkCharcoal
    ),
    // Body Text (The Letters)
    bodyLarge = TextStyle(
        fontFamily = LibreFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        color = InkCharcoal
    ),
    // Button Text
    labelLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = PaperWhite
    ),
    // Small labels (Timestamps, "To:" headers)
    bodySmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = FadedGray
    )
)