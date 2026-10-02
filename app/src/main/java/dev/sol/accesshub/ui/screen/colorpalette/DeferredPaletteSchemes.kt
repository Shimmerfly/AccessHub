package dev.sol.accesshub.ui.screen.colorpalette

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.sol.accesshub.ui.theme.keyColorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * A generated set of preview palettes. Immutable once built, so composables taking it stay
 * skippable — a plain [Map] parameter would be seen as unstable and recompose every frame.
 */
@Immutable
internal class PaletteSchemes(
    private val schemes: Map<Color, ColorScheme>,
) {
    operator fun get(keyColor: Color): ColorScheme? = schemes[keyColor]

    companion object {
        /** Stand-in handed out until the first palettes are generated off the main thread. */
        val Empty = PaletteSchemes(emptyMap())
    }
}

/**
 * Same palette as MaterialKolor's `rememberDynamicColorScheme`, but generated on
 * [Dispatchers.Default] instead of inside composition.
 *
 * MaterialKolor's `remember` variant runs the whole Material 3 palette generation (six tonal
 * palettes plus ~50 colour roles) on the main thread during the very first frame of the page,
 * which makes the page-enter transition stutter. Here the generation happens off the main
 * thread and the previous value stays on screen until the new one is ready.
 *
 * @return `null` until the first palette is ready; callers show a cheap stand-in for those frames.
 */
/**
 * Process-wide cache for generated palettes. Entering the theme page used to rebuild a full scheme
 * per visible swatch (and a background warm-up rebuilt all of them again), which churned ~22MB of
 * garbage per visit; the same inputs now only ever pay for one generation.
 */
private val paletteSchemeCache = java.util.concurrent.ConcurrentHashMap<PaletteKey, ColorScheme>()

private data class PaletteKey(
    val color: Color,
    val isDark: Boolean,
    val isAmoled: Boolean,
    val style: PaletteStyle,
    val specVersion: ColorSpec.SpecVersion,
)

/** Synchronous, cached counterpart of [rememberDynamicColorScheme] for one key colour. */
internal fun cachedColorScheme(
    color: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    style: PaletteStyle,
    specVersion: ColorSpec.SpecVersion,
): ColorScheme = paletteSchemeCache.getOrPut(
    PaletteKey(color, isDark, isAmoled, style, specVersion)
) {
    // Only the seed goes in: passing the dynamic-colour overrides would replace every role with
    // Color.Unspecified and wash the whole palette out to grey.
    dynamicColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = isAmoled,
        style = style,
        specVersion = specVersion,
    )
}

/** Long enough for the push transition to finish, so palette building never steals its frames. */
private const val PALETTE_SCHEME_DELAY_MILLIS = 350L

@Composable
internal fun rememberDeferredColorScheme(
    seedColor: Color,
    isDark: Boolean,
    style: PaletteStyle,
    specVersion: ColorSpec.SpecVersion,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
): ColorScheme? {
    var scheme by remember { mutableStateOf<ColorScheme?>(null) }
    LaunchedEffect(
        seedColor,
        isDark,
        style,
        specVersion,
        primary,
        secondary,
        tertiary,
        neutral,
        neutralVariant,
        error,
    ) {
        delay(PALETTE_SCHEME_DELAY_MILLIS)
        scheme = withContext(Dispatchers.Default) {
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = isDark,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                neutral = neutral,
                neutralVariant = neutralVariant,
                error = error,
                style = style,
                specVersion = specVersion,
            )
        }
    }
    return scheme
}

/**
 * Every key-colour preview palette at once, generated on [Dispatchers.Default].
 *
 * One generation pass feeds both the theme preview card and the swatch row, so scrolling the row
 * (and recomposing it) never generates a palette again. Keyed by the key colour, with
 * [Color.Unspecified] for the wallpaper-dynamic "default" entry, and the dynamic base colours are
 * the ones from `dynamicLightColorScheme` / `dynamicDarkColorScheme`.
 *
 * @return an empty set until the first palettes are ready; callers show cheap stand-ins for those
 *   frames and use the palettes as soon as they land.
 */
@Composable
internal fun rememberDeferredKeyColorSchemes(
    isDark: Boolean,
    style: PaletteStyle,
    specVersion: ColorSpec.SpecVersion,
    dynamicPrimary: Color? = null,
    dynamicSecondary: Color? = null,
    dynamicTertiary: Color? = null,
    dynamicNeutral: Color? = null,
    dynamicNeutralVariant: Color? = null,
    dynamicError: Color? = null,
): PaletteSchemes {
    var schemes by remember { mutableStateOf(PaletteSchemes.Empty) }
    LaunchedEffect(
        isDark,
        style,
        specVersion,
        dynamicPrimary,
        dynamicSecondary,
        dynamicTertiary,
        dynamicNeutral,
        dynamicNeutralVariant,
        dynamicError,
    ) {
        delay(PALETTE_SCHEME_DELAY_MILLIS)
        schemes = withContext(Dispatchers.Default) {
            PaletteSchemes(
                buildMap<Color, ColorScheme> {
                    put(
                        Color.Unspecified,
                        dynamicColorScheme(
                            seedColor = Color.Unspecified,
                            isDark = isDark,
                            primary = dynamicPrimary,
                            secondary = dynamicSecondary,
                            tertiary = dynamicTertiary,
                            neutral = dynamicNeutral,
                            neutralVariant = dynamicNeutralVariant,
                            error = dynamicError,
                            style = style,
                            specVersion = specVersion,
                        ),
                    )
                    keyColorOptions.forEach { keyColor ->
                        // Spread the work out: one burst of 17 full schemes saturated the CPU while the
                        // enter transition was still running, which made the first visits stutter.
                        delay(24)
                        put(
                            Color(keyColor),
                            dynamicColorScheme(
                                seedColor = Color(keyColor),
                                isDark = isDark,
                                style = style,
                                specVersion = specVersion,
                            ),
                        )
                    }
                }
            )
        }
    }
    return schemes
}
