package com.face.attendancesystem.face_recognonize

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.Rect
import android.text.TextUtils
import android.util.Log
import com.face.attendancesystem.camerax.GraphicOverlay
import com.google.mlkit.vision.face.Face


class FaceContourGraphic(
    overlay: GraphicOverlay,
    private val face: Face,
    private val imageRect: Rect,
    private val onSuccessCallback: ((FaceStatus) -> Unit),
) : GraphicOverlay.Graphic(overlay) {

    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint
    var name:String = ""
    lateinit var idPaints: Array<Paint>
    val margin:Int = 5

    private val COLORS = arrayOf(
        intArrayOf(Color.BLACK, Color.WHITE),
        intArrayOf(Color.WHITE, Color.MAGENTA),
        intArrayOf(Color.BLACK, Color.LTGRAY),
        intArrayOf(Color.WHITE, Color.RED),
        intArrayOf(Color.WHITE, Color.BLUE),
        intArrayOf(Color.WHITE, Color.DKGRAY),
        intArrayOf(Color.BLACK, Color.CYAN),
        intArrayOf(Color.BLACK, Color.YELLOW),
        intArrayOf(Color.WHITE, Color.BLACK),
        intArrayOf(Color.BLACK, Color.GREEN)
    )

    init {
        val selectedColor = Color.WHITE

        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor
        idPaint = Paint()
        idPaint.color = selectedColor

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
        val numColors: Int = COLORS.size

    }
    private val greenBoxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
    }

    private val redBoxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
    }

    var fm = FontMetrics()
    private val textColor = Paint().apply {
        color = Color.GREEN
        textSize = ID_TEXT_SIZE
        getFontMetrics(fm)
    }

    override fun draw(canvas: Canvas?) {

        val left = face.boundingBox.left.toFloat()
        val right = face.boundingBox.right.toFloat()
        val top = face.boundingBox.top.toFloat()
        val bottom = face.boundingBox.bottom.toFloat()
        val rect = calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
            face.boundingBox
        )
        val faceDimensions = getFaceDimensions()

        val lineHeight: Float =
            ID_TEXT_SIZE + BOX_STROKE_WIDTH
        var yLabelOffset: Float = if (face.trackingId == null) 0f else -lineHeight

        when {
            checkIsToFar(faceDimensions) -> {
                onSuccessCallback(FaceStatus.TOO_FAR)
                canvas?.drawRect(rect,redBoxPaint)
            }
            checkIsNoCentered(faceDimensions) -> {
                onSuccessCallback(FaceStatus.NOT_CENTERED)
                canvas?.drawRect(rect,redBoxPaint)
            }
            else -> {
                onSuccessCallback(FaceStatus.VALID)
                canvas?.drawRect(rect,greenBoxPaint)
            }
        }

        if (face.trackingId != null) {
            Log.e("TAG", "drawId:${face.trackingId}")
           /* canvas!!.drawText(
                "ID: " + face.trackingId,
                face.boundingBox.left.toFloat(),
                face.boundingBox.top + yLabelOffset,
                textColor
            )
            yLabelOffset += lineHeight*/
        }

        if (!TextUtils.isEmpty(name)){
//            canvas?.drawRect(left-BOX_STROKE_WIDTH, top,left+textColor.measureText(name)+(2*BOX_STROKE_WIDTH),top,textColor)
            canvas?.drawRect(left- BOX_STROKE_WIDTH, top + fm.top - margin,
                left + textColor.measureText("Name: $name") + (2* BOX_STROKE_WIDTH), top + fm.bottom
                        + margin, textColor);
            Log.e("TAG", "drawText: $name")
            textColor.color = Color.WHITE
            canvas?.drawText("Name: $name",
                face.boundingBox.left.toFloat(), face.boundingBox.top.toFloat(),textColor)
        }
    }
    companion object {
        private const val BOX_STROKE_WIDTH = 5.0f
        private const val ID_TEXT_SIZE = 30.0f
    }


    private fun checkIsNoCentered(
        faceDimensions: FaceDimensions
    ): Boolean {
        val width =  imageRect.width()
        val height =  imageRect.height()

        return faceDimensions.left < 0 || faceDimensions.right > width || faceDimensions.top < 0 || faceDimensions.bottom > height
    }

    private fun checkIsToFar(
        faceDimensions: FaceDimensions
    ): Boolean {
        val screenPercentage = 0.4f
        val width =  imageRect.width()
        val height =  imageRect.height()

        return (
                faceDimensions.bottom - faceDimensions.top <= height * screenPercentage ||
                        faceDimensions.right - faceDimensions.left <= width * screenPercentage
                )
    }

    private fun getFaceDimensions(): FaceDimensions = run {
        val x = face.boundingBox.centerX().toFloat()
        val y = face.boundingBox.centerY().toFloat()
        FaceDimensions(
            x = x,
            y = y,
            left = x - face.boundingBox.width() / 2.0f,
            top = y - face.boundingBox.height() / 2.0f,
            right = x + face.boundingBox.width() / 2.0f,
            bottom = y + face.boundingBox.height() / 2.0f,
        )
    }
}

data class FaceDimensions(
    val x: Float,
    val y: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)