package com.example.taller_2_cm

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val Imagen1 = findViewById<ImageButton>(R.id.imageButton)
        val Imagen2 = findViewById<ImageButton>(R.id.imageButton2)
        val Imagen3 = findViewById<ImageButton>(R.id.imageButton3)

        irAContactos(Imagen1)
        irACamara(Imagen2)
        irAMapa(Imagen3)
    }

    private fun irAContactos(Imagen1: ImageButton){
        Imagen1.setOnClickListener {
            val intent = Intent(this, Contactos::class.java)
            startActivity(intent)
        }
    }

    private fun irACamara(Imagen2: ImageButton){
        Imagen2.setOnClickListener {
            val intent = Intent(this, Camara::class.java)
            startActivity(intent)
            }
    }

    private fun irAMapa(Imagen3: ImageButton) {
        Imagen3.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}