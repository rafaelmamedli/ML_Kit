package com.rafael.mlkit

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.rafael.mlkit.databinding.ActivityMainBinding
import java.lang.Exception
import android.net.Uri
import android.provider.Settings


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val CAMERA_PERMISSION_CODE = 123
    private val STORAGE_PERMISSION_CODE = 1


    private lateinit var cameraLauncher:ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher:ActivityResultLauncher<Intent>

    lateinit var inputImage: InputImage
    lateinit var imageLabler:ImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        imageLabler= ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult?>{
                override fun onActivityResult(result: ActivityResult?) {
                    val data = result?.data
                    try {
                        val photo= data?.extras?.get("data") as Bitmap
                        binding.imageView.setImageBitmap(photo)
                        inputImage= InputImage.fromBitmap(photo,0)
                        processImage()
                    } catch (e:Exception) {

                    }
                }

            }
        )

        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result?.data
            try {
                inputImage = InputImage.fromFilePath(this@MainActivity, data?.data!!)
                binding.imageView.setImageURI(data?.data)
                processImage()
            } catch (e: Exception) {

            }
        }


        binding.button.setOnClickListener {
            val options= arrayOf("Camera","Gallery")
            val builder= AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Pick a option")
            builder.setItems(options,DialogInterface.OnClickListener { dialog, which ->
                if (which == 0) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                } else {
                    val storageIntent= Intent()
                    storageIntent.type = "image/*"
                    storageIntent.action = Intent.ACTION_GET_CONTENT
                    galleryLauncher.launch(storageIntent)
                }
            })

           builder.show()

        }
    }



    private fun processImage() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            imageLabler.process(inputImage).addOnSuccessListener {
                var result= ""
                for (label in it) {
                    val text = label.text
                    result=result+"\n" + text
                }
                binding.tvResult.text = result
            }.addOnCanceledListener {
            }
        } else {
            openAppSettings()
        }
    }



    override fun onResume() {
        super.onResume()
            checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE)
    }



    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            val rationale = when (permission) {
                android.Manifest.permission.CAMERA -> "Permission is required to access the camera"
                android.Manifest.permission.READ_EXTERNAL_STORAGE -> "Access to storage requires permission."
                else -> ""
            }
            if (shouldShowRequestPermissionRationale(permission)) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Permission needed")
                    .setMessage(rationale)
                    .setPositiveButton("Ok") { _, _ ->
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            }
        } else {
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
            }
        }
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
            }
        }
    }


    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)

        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}
