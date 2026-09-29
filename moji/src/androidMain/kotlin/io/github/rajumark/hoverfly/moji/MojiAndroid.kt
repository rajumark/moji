@file:JvmName("MojiAndroid")

package io.github.rajumark.hoverfly.moji

import android.content.Context

/** Kept so 1.x code (`Moji(context)`) still compiles; the model no longer needs a [Context]. */
@Deprecated("The model is bundled without assets now; use Moji().", ReplaceWith("Moji()"))
@Suppress("UNUSED_PARAMETER", "FunctionName")
public fun Moji(context: Context): Moji = Moji()
