package com.face.attendancesystem.face_recognonize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.util.Pair
import androidx.camera.core.ImageProxy
import com.face.attendancesystem.R
import com.face.attendancesystem.camerax.BaseImageAnalyzer
import com.face.attendancesystem.camerax.GraphicOverlay
import com.face.attendancesystem.model.Person
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileDescriptor
import java.io.IOException
import kotlin.math.sqrt


class FaceContourDetectionProcessor(
    private val context: Context,
    private val view: GraphicOverlay,
    private val interpreter: Interpreter,
    private val onSuccessCallback: ((FaceStatus) -> Unit),
    private val onDetectCallback: ((Bitmap, Float) -> Unit)
) : BaseImageAnalyzer<List<Face>>() {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .build()

    private var listOfPerson: ArrayList<Bitmap> = arrayListOf()

    private val faceNetImageProcessor = ImageProcessor.Builder()
        .add(
            ResizeOp(
                FACENET_INPUT_IMAGE_SIZE,
                FACENET_INPUT_IMAGE_SIZE,
                ResizeOp.ResizeMethod.BILINEAR
            )
        )
        .add(NormalizeOp(0f, 255f))
        .build()


    private val detector = FaceDetection.getClient(realTimeOpts)

    private var empArray: MutableList<FloatArray> = mutableListOf()
    var recognisedFaceList: ArrayList<Person> = arrayListOf()

    override val graphicOverlay: GraphicOverlay
        get() = view

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun employeeImg() {
        detectEmployee()
    }

    private fun detectEmployee() {
        if (recognisedFaceList.isEmpty() && listOfPerson.isEmpty()) {
            val person1Images = drawableToBitmap(context, R.drawable.person1)
            val person2mages = drawableToBitmap(context, R.drawable.person2)
            val person3mages = drawableToBitmap(context, R.drawable.person3)
            val person4mages = drawableToBitmap(context, R.drawable.fourth)
            val person5mages = drawableToBitmap(context, R.drawable.five)
            val personListOf =
                arrayListOf(person1Images, person2mages, person3mages, person4mages, person5mages)

            listOfPerson.addAll(personListOf)
        }

//         val imageBitmap = uriToBitmap(Uri.fromFile(File(empImg)))

        if (listOfPerson.isNotEmpty() && recognisedFaceList.isEmpty()) {

            listOfPerson.forEach { imageBitmap ->
                val inputImg = InputImage.fromBitmap(imageBitmap, 0)
                detector.process(inputImg).addOnSuccessListener { result ->
                    if (result.isNotEmpty()) {
                        result.forEach {
                            val faceBitmap =
                                cropToBox(imageBitmap, it.boundingBox, inputImg.rotationDegrees)
                                    ?: return@forEach
                            val tensorImg = TensorImage.fromBitmap(faceBitmap)
                            val faceOutputArray = Array(1) {
                                FloatArray(
                                    192
                                )
                            }
                            val faceNetByteBuffer = faceNetImageProcessor.process(tensorImg).buffer
                            interpreter.run(faceNetByteBuffer, faceOutputArray)

                            recognisedFaceList.add(
                                Person(
                                    imageBitmap.generationId.toString(),
                                    faceOutputArray[0]
                                )
                            )
                        }
                    }
                }
            }

        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor =
                context.contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
        }
    }

    override fun onSuccess(
        results: List<Face>,
        graphicOverlay: GraphicOverlay,
        rect: Rect,
        imgproxy: ImageProxy,
        bitmap: Bitmap
    ) {
        Log.e("IMAGEARRAY", "onSuccess: ${results}")
        graphicOverlay.clear()
        if (results.isNotEmpty()) {
            results.forEach { result ->
                val faceGraphic = FaceContourGraphic(
                    graphicOverlay, result, rect, onSuccessCallback
                )
                val faceBitmap =
                    cropToBox(bitmap, result.boundingBox, imgproxy.imageInfo.rotationDegrees)
                        ?: return
                val tensorImg = TensorImage.fromBitmap(faceBitmap)
                val faceOutputArray = Array(1) {
                    FloatArray(
                        192
                    )
                }
                val faceNetByteBuffer = faceNetImageProcessor.process(tensorImg).buffer
                interpreter.run(faceNetByteBuffer, faceOutputArray)

                if (recognisedFaceList.isNotEmpty()) {
                    Log.e(TAG, "listnotEmpty:$recognisedFaceList")
                    val result: Pair<String, Float> = findNearestFace(
                        faceOutputArray[0]
                    ) ?: return

                    if (result.second < 0.90f) {
                        Log.e(TAG, "onSuccessResult:$result ")
                        faceGraphic.name = result.second.toString()
                        onDetectCallback.invoke(faceBitmap, result.second)
                    }

                }
                graphicOverlay.add(faceGraphic)
            }
            graphicOverlay.postInvalidate()
        } else {
            onSuccessCallback(FaceStatus.NO_FACE)
        }
    }


    fun drawableToBitmap(context: Context, drawableId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, drawableId)
    }

    private fun findNearestFace(vector: FloatArray): Pair<String, Float>? {
        var ret: Pair<String, Float>? = null
        for (person in recognisedFaceList) {
            val name: String = person.name!!
            val knownVector: FloatArray = person.faceVector!!
            var distance = 0f
            for (i in vector.indices) {
                val diff = vector[i] - knownVector[i]
                distance += diff * diff
            }
            Log.e(TAG, "findNearestFace:distance :$distance")
            distance = sqrt(distance.toDouble()).toFloat()
            if (ret == null || distance < ret.second) {
                Log.e(TAG, "retention:$ret ")
                ret = Pair(name, distance)
            }
        }
        Log.e(TAG, "findNearestFace Result $ret")
        return ret
    }

    private fun cropToBox(image: Bitmap, boundingBox: Rect, rotation: Int): Bitmap? {
        var image = image
        val shift = 0
        if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            image = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
        }
        return if (boundingBox.top >= 0 && boundingBox.bottom <= image.width && boundingBox.top + boundingBox.height() <= image.height && boundingBox.left >= 0 && boundingBox.left + boundingBox.width() <= image.width) {
            Bitmap.createBitmap(
                image,
                boundingBox.left,
                boundingBox.top + shift,
                boundingBox.width(),
                boundingBox.height()
            )
        } else null
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Face Detector failed. $e")
        onSuccessCallback(FaceStatus.NO_FACE)
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
        private const val FACENET_INPUT_IMAGE_SIZE = 112
    }

}