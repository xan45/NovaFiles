package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import kotlinx.coroutines.launch

data class AuthenticOnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val highlights: List<String>
)

val authenticOnboardingPages = listOf(
    AuthenticOnboardingPage(
        title = "Dual-Pane Power Browser",
        subtitle = "TOTAL COMMANDER WORKFLOW",
        description = "Browse two folders simultaneously on phones and tablets. Effortlessly move or copy files across independent panels with a single tap.",
        icon = Icons.AutoMirrored.Filled.CompareArrows,
        iconColor = SecondaryTeal,
        highlights = listOf(
            "Side-by-side management on phones & tablets",
            "1-tap fast copy & move transfers between panels",
            "Independent sorting, view modes, and breadcrumbs"
        )
    ),
    AuthenticOnboardingPage(
        title = "Safe Storage Analyzer",
        subtitle = "3-POINT SHA-256 CLEANER",
        description = "Deeply analyze disk usage by category. Eliminate duplicates safely with multi-point cryptographic hashing that guarantees zero false positives.",
        icon = Icons.Default.PieChart,
        iconColor = TertiaryAmber,
        highlights = listOf(
            "3-Point sampling (64KB Head + Mid + Tail) with SHA-256",
            "Visual Donut and Bar category storage breakdowns",
            "Junk cache and empty folder 1-tap cleaner"
        )
    ),
    AuthenticOnboardingPage(
        title = "Universal Archive Suite",
        subtitle = "7Z, RAR, TAR & ZIP SUPPORT",
        description = "Comprehensive compressed file support. Inspect contents, verify CRC checksums, and extract 7-Zip, WinRAR, UNIX TAR/GZ, and ZIP archives directly.",
        icon = Icons.Default.FolderZip,
        iconColor = Color(0xFF8B5CF6),
        highlights = listOf(
            "7-Zip (.7z) decompressed with Apache Commons Compress",
            "WinRAR (.rar) extraction powered by Junrar",
            "In-archive browsing without full extraction"
        )
    ),
    AuthenticOnboardingPage(
        title = "AES-256 Encrypted Vault",
        subtitle = "ZERO-KNOWLEDGE PRIVACY",
        description = "Hardware-grade private storage. Encrypt sensitive documents, images, and videos with AES/CBC/PKCS5Padding protected by a salted PIN passcode.",
        icon = Icons.Default.Lock,
        iconColor = Color(0xFFEF4444),
        highlights = listOf(
            "Unique per-file Initialization Vectors (IVs)",
            "Salted SHA-256 PIN key derivation",
            "Stealth storage hidden from media indexers"
        )
    ),
    AuthenticOnboardingPage(
        title = "Windows Share & Wi-Fi FTP",
        subtitle = "SMBv2/v3 & CABLE-FREE TRANSFER",
        description = "Connect directly to Windows 10/11 network shares and NAS devices via SMBv2/SMBv3, or run a high-speed Wi-Fi FTP server on your phone.",
        icon = Icons.Default.Lan,
        iconColor = PrimaryBlue,
        highlights = listOf(
            "Native SMBv2 & SMBv3 client powered by smbj",
            "Built-in multi-threaded Wi-Fi FTP server on port 2121",
            "Browse remote shares and download directly to phone"
        )
    ),
    AuthenticOnboardingPage(
        title = "Root & Shizuku Explorer",
        subtitle = "PRIVILEGED SYSTEM ACCESS",
        description = "Inspect restricted partitions (/data, /system, /vendor) with elevated superuser shell execution ('su') and Shizuku binder authorization.",
        icon = Icons.Default.Security,
        iconColor = Color(0xFF10B981),
        highlights = listOf(
            "Root superuser shell runner with live terminal console",
            "Shizuku service status and permission verification",
            "UNIX permissions, file modes, and partition inspection"
        )
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { authenticOnboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NovaFiles Tour",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = onFinish) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageData = authenticOnboardingPages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Icon Box
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(pageData.iconColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pageData.icon,
                            contentDescription = null,
                            tint = pageData.iconColor,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = pageData.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = pageData.iconColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = pageData.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Highlights Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pageData.highlights.forEach { highlight ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = pageData.iconColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(authenticOnboardingPages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width = animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "indicator"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width.value)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                // Next or Get Started Button
                val isLastPage = pagerState.currentPage == authenticOnboardingPages.size - 1

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isLastPage) "Get Started" else "Next")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
