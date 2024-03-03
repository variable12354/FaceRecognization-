package com.face.attendancesystem

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.face.attendancesystem.camerax.CameraManager
import com.face.attendancesystem.databinding.ActivityMainBinding
import com.face.attendancesystem.face_recognonize.FaceStatus
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bitmap: Bitmap? = null
    private var result:Float? = null
    private var selfieImg:String? = null
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        createCameraManager()
        checkForPermission()
        onClicks()
    }


    private fun checkForPermission() {
        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun onClicks() = with(binding) {
        btnSwitchCamera.setOnClickListener {
            cameraManager.changeCameraSelector()
        }
        btnRecord.setOnClickListener {
            Log.e("TAG", "onClick:$result")
            val resulting = if (result != null) result else 0.0f
            val verify = if (resulting?.equals(0.0f) == true){ false }
            else if(resulting!! < 1.0f){ true }
            else{ false }
            val bStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bStream)
            val byteArray = bStream.toByteArray()
            setResult(RESULT_OK, Intent().apply {
                putExtra(FACEDATA,resulting)
                putExtra(CAPTUREIMG,byteArray)
                putExtra(VERIFIED,verify ?: false)
            })
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults:
    IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraManager.startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun createCameraManager() {
        cameraManager = CameraManager(
            this,
            binding.previewViewFinder,
            this,
            binding.graphicOverlayFinder,
            ::processPicture,
            ::onFaceDetect
        )
    }

    private fun onFaceDetect(bitmap: Bitmap,result: Float){
        this.bitmap = bitmap
        this.result = result
        if (result < 1.0f) {
            Toast.makeText(this, "Verified User", Toast.LENGTH_SHORT).show()
        }
        Log.e("TAG", "onFaceDetect: bitmap : $bitmap result : $result" )
    }


    private fun processPicture(faceStatus: FaceStatus) {}

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
        const val FACEDATA = "facedata"
        const val VERIFIED = "verify"
        const val EMPLOYEEIMG = "employeeImg"
        const val CAPTUREIMG  = "captureimg"
    }
}