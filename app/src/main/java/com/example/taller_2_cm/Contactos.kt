package com.example.taller_2_cm

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Contactos : AppCompatActivity() {

    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        mlista = findViewById<ListView>(R.id.lista)
        mProjection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
        mContactsAdapter = ContactsAdapter(this, null, 0)
        mlista?.adapter = mContactsAdapter

        PedirPermisoContactos(this, android.Manifest.permission.READ_CONTACTS, "Se necesita de este permiso",
            Data.Companion.MY_PERMISSION_REQUEST_READ_CONTACTS)
    }

    fun initView() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            mCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
            )
            mContactsAdapter?.changeCursor(mCursor)
        }
    }
    private fun PedirPermisoContactos(context: Activity, permiso: String, justificacion: String, idCode: Int) {
        //Pedir permisos
        if (ContextCompat.checkSelfPermission(
                context,
                permiso
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permiso)) {

            }
            ActivityCompat.requestPermissions(context, arrayOf(permiso), idCode)
        } else {
            initView()

        }
    }


        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            when (requestCode) {
                Data.Companion.MY_PERMISSION_REQUEST_READ_CONTACTS -> {
                    if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "permiso concedido", Toast.LENGTH_SHORT).show()
                        initView()
                    } else {
                        Toast.makeText(this, "permiso denegado", Toast.LENGTH_SHORT).show()

                    }
                    return
                }
                else -> {
                    // Ignore all other requests.
                }
            }
        }
    }
}