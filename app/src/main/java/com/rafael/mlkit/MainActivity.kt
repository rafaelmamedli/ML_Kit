package com.rafael.mlkit

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.lang.Exception
import com.rafael.mlkit.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    companion object{
        const val PERMISSION_REQUEST_CAMERA = 1001
        const val PERMISSION_REQUEST_STORAGE = 1002
    }
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    lateinit var inputImage: InputImage
    lateinit var imageLabler: ImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        imageLabler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result?.data
            try {
                val photo = data?.extras?.get("data") as Bitmap
                binding.imageView.setImageBitmap(photo)
                inputImage = InputImage.fromBitmap(photo, 0)
                processImage()
            } catch (e: Exception) {
                println(e)
            }
        }

        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result?.data
            try {
                inputImage = InputImage.fromFilePath(this@MainActivity, data?.data!!)
                binding.imageView.setImageURI(data.data)
                processImage()
            } catch (e: Exception) {
                println(e)
            }
        }


        binding.button.setOnClickListener {
            showOptionsDialog()
        }

    }


    private fun processImage() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            imageLabler.process(inputImage).addOnSuccessListener {
                var result = ""
                for (label in it) {
                    val text = label.text
                    result = result + "\n" + text
                }
                binding.tvResult.text = result
            }.addOnCanceledListener {
            }
        }
    }

    private fun showOptionsDialog() {
        val options = arrayOf("Gallery", "Camera")
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Pick an option")
        builder.setItems(options) { dialog, which ->
            when (which) {
                1 -> {
                    if (hasCameraPermission()) {
                        openCamera()
                    } else {
                        requestCameraPermission()
                    }
                }
                0 -> {
                    if (hasStoragePermission()) {
                        openGallery()
                    } else {
                        requestStoragePermission()
                    }
                }
            }
        }
        builder.show()
    }


    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_STORAGE)
    }

    private fun openGallery() {
        val storageIntent = Intent()
        storageIntent.type = "image/*"
        storageIntent.action = Intent.ACTION_GET_CONTENT
        galleryLauncher.launch(storageIntent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Log.d("TAG","Permission denied for Camera")

                }
            }
            PERMISSION_REQUEST_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Log.d("TAG","Permission denied for Media")
                }
            }
        }
    }
}
