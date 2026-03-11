package com.saiyan.dragonballuniverse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import com.saiyan.dragonballuniverse.R

/**
 * Typography:
 * - ربط خط Cairo العربي عبر Google Fonts.
 */
private val CairoGoogleFont = GoogleFont("Cairo")

/**
 * Google Font Provider (استخدم شهادات جوجل الافتراضية عبر مكتبة ui-text-google-fonts).
 *
 * ملاحظة: يتطلب وجود certs داخل res/font:
 * - com_google_android_gms_fonts_certs.xml
 * - com_google_android_gms_fonts_certs_dev.xml
 *
 * لو لم تكن موجودة حالياً، سنضيفها في الخطوة القادمة داخل المشروع.
 */
private val GoogleFontsProvider: Provider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val AppFontFamily = FontFamily(
    Font(googleFont = CairoGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = CairoGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = CairoGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = CairoGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)
