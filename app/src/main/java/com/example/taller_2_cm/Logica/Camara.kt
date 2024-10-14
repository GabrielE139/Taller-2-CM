package com.example.taller_2_cm.Logica

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller_2_cm.Datos.Data
import com.example.taller_2_cm.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class Camara : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        imageView = findViewById(R.id.imageCamara)

        val permisos = arrayOf( Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,  Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val justificaciones = arrayOf( "Se necesita acceso a la cámara para tomar fotos",  "Se necesita acceso a la galería para seleccionar fotos",  "Se necesita acceso de escritura para guardar fotos" )
        val idCodes = arrayOf( Data.MY_PERMISSION_REQUEST_CAMERA,  Data.MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE,  Data.MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE )

        pedirPermisos(permisos, justificaciones, idCodes)

        val buttonGaleria = findViewById<Button>(R.id.buttonGaleria)
        val buttonCamara = findViewById<Button>(R.id.buttonCamara)

        buttonCamara.setOnClickListener {
            openCamera()
        }

        buttonGaleria.setOnClickListener {
            dispatchPickPictureIntent()
        }

    }

    private fun pedirPermisos(permisos: Array<String>, justificaciones: Array<String>, idCodes: Array<Int>) {
        val permisosNoConcedidos = mutableListOf<String>()

        for (i in permisos.indices) {
            if (ContextCompat.checkSelfPermission(this, permisos[i]) != PackageManager.PERMISSION_GRANTED) {
                permisosNoConcedidos.add(permisos[i])
            }
        }

        if (permisosNoConcedidos.isNotEmpty()) {
            for (permiso in permisosNoConcedidos) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permiso)) {
                    val index = permisos.indexOf(permiso)
                    Toast.makeText(this, justificaciones[index], Toast.LENGTH_LONG).show()
                }
            }
            ActivityCompat.requestPermissions(this, permisosNoConcedidos.toTypedArray(), Data.MY_PERMISSION_REQUEST_MULTIPLE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val buttonGaleria = findViewById<Button>(R.id.buttonGaleria)
        val buttonCamara = findViewById<Button>(R.id.buttonCamara)

        for (i in permissions.indices) {
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permisos para la cámara concedidos", Toast.LENGTH_SHORT).show()
                        buttonCamara.isEnabled = true
                    } else {
                        Toast.makeText(this, "Permisos para la cámara denegados", Toast.LENGTH_SHORT).show()
                        buttonCamara.isEnabled = false
                    }
                }
                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permisos para la galería concedidos", Toast.LENGTH_SHORT).show()
                        buttonGaleria.isEnabled = true
                    } else {
                        Toast.makeText(this, "Permisos para la galería denegados", Toast.LENGTH_SHORT).show()
                        buttonGaleria.isEnabled = false
                    }
                }
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        buttonGaleria.isEnabled = true
                        Toast.makeText(this, "Permisos de escritura concedidos", Toast.LENGTH_SHORT).show()
                    } else {
                        buttonGaleria.isEnabled = false
                        Toast.makeText(this, "Permisos de escritura denegados", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.taller_2_cm.fileprovider",
                it
            )
            galleryAddPic()
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, Data.MY_PERMISSION_REQUEST_CAMERA)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(currentPhotoPath)
            val contentUri: Uri = Uri.fromFile(f)
            mediaScanIntent.data = contentUri
            sendBroadcast(mediaScanIntent)
        }
    }

    private fun dispatchPickPictureIntent() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { pickPictureIntent ->
            pickPictureIntent.type = "image/*"
            startActivityForResult(pickPictureIntent, Data.MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Data.MY_PERMISSION_REQUEST_CAMERA -> {
                    val file = File(currentPhotoPath)
                    if (file.exists()) {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                        imageView.setImageBitmap(bitmap)
                    }
                }
                Data.MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
                    val selectedImageUri = data?.data
                    imageView.setImageURI(selectedImageUri)
                }
            }
        }
    }
}