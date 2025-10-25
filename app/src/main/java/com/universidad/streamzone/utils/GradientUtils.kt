package com.universidad.streamzone.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Utilidades para aplicar gradientes programáticamente
 */
object GradientUtils {

    /**
     * Aplica un gradiente circular a una vista
     *
     * @param view Vista a la que se aplicará el gradiente
     * @param startColorHex Color inicial en formato hex (ej: "#ff4b4b")
     * @param endColorHex Color final en formato hex (ej: "#b20a0a")
     * @param cornerRadius Radio de las esquinas en pixels
     */
    fun applyGradient(
        view: View,
        startColorHex: String,
        endColorHex: String,
        cornerRadius: Float = 0f
    ) {
        try {
            val startColor = Color.parseColor(startColorHex)
            val endColor = Color.parseColor(endColorHex)

            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, // Top-Left to Bottom-Right (135°)
                intArrayOf(startColor, endColor)
            )

            gradientDrawable.cornerRadius = cornerRadius

            view.background = gradientDrawable

        } catch (e: IllegalArgumentException) {
            // Color inválido, usar color por defecto
            val defaultGradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#6b7280"),
                    Color.parseColor("#374151")
                )
            )
            defaultGradient.cornerRadius = cornerRadius
            view.background = defaultGradient
        }
    }

    /**
     * Convierte dp a pixels
     */
    fun dpToPx(view: View, dp: Float): Float {
        return dp * view.context.resources.displayMetrics.density
    }
}