package com.universidad.streamzone.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Convierte una cadena Base64 a un objeto Bitmap.
     * Retorna null si la cadena es nula, vacía o inválida.
     */
    fun convertirBase64ABitmap(base64: String?): Bitmap? {
        if (base64.isNullOrEmpty()) return null
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Convierte un objeto Bitmap a una cadena Base64.
     */
    fun convertirBitmapABase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
