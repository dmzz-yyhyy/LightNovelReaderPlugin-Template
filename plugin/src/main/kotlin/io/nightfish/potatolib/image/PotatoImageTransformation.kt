package io.nightfish.potatolib.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import io.nightfish.lightnovelreader.api.image.ImageSize
import io.nightfish.lightnovelreader.api.image.ImageTransformation

class PotatoImageTransformation: ImageTransformation {
    override fun getCacheKey(uri: Uri) = uri.toString()

    override suspend fun transform(
        input: Bitmap,
        size: ImageSize,
        uri: Uri
    ): Bitmap {
        val result = input.copy(
            input.config ?: Bitmap.Config.ARGB_8888,
            true
        )

        val canvas = Canvas(result)

        val text = "🥔 potato"
        val padding = 24f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
        }

        val x = result.width - padding
        val y = padding + paint.textSize

        canvas.drawText(text, x, y, paint)

        return result
    }
}