package edu.umich.argo.arnote

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat.NV21
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/// @param folderName can be your app's name
fun saveImage(bitmap: Bitmap, context: Context, folderName: String): Uri? {
    val values = contentValues()
    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
    values.put(MediaStore.Images.Media.IS_PENDING, true)
    // RELATIVE_PATH and IS_PENDING are introduced in API 29.

    val uri: Uri? = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values)
    if (uri != null) {
        saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
        values.put(MediaStore.Images.Media.IS_PENDING, false)
        context.contentResolver.update(uri, values, null, null)
    }
    return uri
}

private fun contentValues() : ContentValues {
    val values = ContentValues()
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
    values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
    return values
}

private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
    if (outputStream != null) {
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun yuv_420_888toNV21(image: Image): ByteArray {
    val nv21: ByteArray
    val yBuffer: ByteBuffer = image.planes[0].buffer
    val uBuffer: ByteBuffer = image.planes[1].buffer
    val vBuffer: ByteBuffer = image.planes[2].buffer
    val ySize: Int = yBuffer.remaining()
    val uSize: Int = uBuffer.remaining()
    val vSize: Int = vBuffer.remaining()
    nv21 = ByteArray(ySize + uSize + vSize)

    //U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    return nv21
}

fun nv21toJPEG(nv21: ByteArray, width: Int, height: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val yuv = YuvImage(nv21, NV21, width, height, null)
    yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
    return out.toByteArray()
}
