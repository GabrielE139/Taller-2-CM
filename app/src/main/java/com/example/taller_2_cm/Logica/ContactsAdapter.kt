package com.example.taller_2_cm.Logica

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import com.example.taller_2_cm.R

class ContactsAdapter(context: Context?, c: Cursor?, flags: Int) : CursorAdapter(context, c, flags) {
    private val CONTACT_ID_INDEX = 0
    private val DISPLAY_NAME_INDEX = 1
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.layout_contacts_adapter, parent, false)
    }
    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val tvIdContacto = view?.findViewById<TextView>(R.id.number)
        val tvNombre = view?.findViewById<TextView>(R.id.name)
        val idnum = cursor?.getInt(CONTACT_ID_INDEX)
        val nombre = cursor?.getString(DISPLAY_NAME_INDEX)
        tvIdContacto?.text = idnum?.toString()
        tvNombre?.text = nombre
    }

}