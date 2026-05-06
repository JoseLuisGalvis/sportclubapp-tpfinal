package com.sportclub.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

class FotoManager(val context: Context) {

    companion object {
        private const val MAX_DIM      = 400
        private const val JPEG_QUALITY = 85
    }

    var archivoCamaraTemp: File? = null
        private set

    fun crearUriCamara(prefijo: String = "foto"): Uri? {
        return runCatching {
            val archivo = File(context.cacheDir, "${prefijo}_${System.currentTimeMillis()}.jpg")
            archivoCamaraTemp = archivo
            FileProvider.getUriForFile(context, "${context.packageName}.provider", archivo)
        }.getOrNull()
    }

    fun procesarDesdeArchivo(): ByteArray? {
        val archivo = archivoCamaraTemp ?: return null
        return runCatching {
            val bmp = BitmapFactory.decodeFile(archivo.absolutePath)
            procesarBitmap(bmp)
        }.getOrNull()
    }

    fun procesarDesdeUri(uri: Uri): ByteArray? {
        return runCatching {
            val stream = context.contentResolver.openInputStream(uri)
            val bmp    = BitmapFactory.decodeStream(stream)
            procesarBitmap(bmp)
        }.getOrNull()
    }

    fun procesarBitmap(bmp: Bitmap): ByteArray {
        val escalado = escalar(bmp, MAX_DIM)
        val stream   = ByteArrayOutputStream()
        escalado.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.toByteArray()
    }

    fun escalar(bmp: Bitmap, maxDim: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w <= maxDim && h <= maxDim) return bmp
        val factor = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bmp, (w * factor).toInt(), (h * factor).toInt(), true)
    }
}