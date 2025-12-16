package com.example.unsaid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unsaid.ui.theme.InkCharcoal
import com.example.unsaid.ui.theme.InterFont
import com.example.unsaid.ui.theme.LibreFont

@Composable
fun LetterCard(
    recipient: String,  // e.g. "To the friend I lost touch with"
    message: String     // e.g. "I still think about our..."
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp), // Space between cards
        shape = RoundedCornerShape(4.dp), // Subtle rounded corners (like cardstock)
        colors = CardDefaults.cardColors(
            // A very light grey, slightly darker than the background to stand out
            containerColor = Color(0xFFF2F0EB)
        ),
        elevation = CardDefaults.cardElevation(0.dp) // Flat. Minimal.
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // Generous inner padding
        ) {
            // 1. The "To:" Header (Sans Serif, small, grey)
            Text(
                text = "To: $recipient",
                fontFamily = InterFont,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color(0xFF666666), // Dark Grey
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 2. The Body (Serif, larger, Charcoal)
            Text(
                text = message,
                fontFamily = LibreFont,
                fontSize = 16.sp,
                lineHeight = 24.sp, // Extra breathing room for lines
                color = InkCharcoal
            )
        }
    }
}