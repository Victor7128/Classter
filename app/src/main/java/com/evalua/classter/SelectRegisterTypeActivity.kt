package com.evalua.classter

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class SelectRegisterTypeActivity : AppCompatActivity() {
    private lateinit var cardAlumno: MaterialCardView
    private lateinit var cardApoderado: MaterialCardView
    private lateinit var cardDocente: MaterialCardView
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_register_type)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        cardAlumno = findViewById(R.id.cardAlumno)
        cardApoderado = findViewById(R.id.cardApoderado)
        cardDocente = findViewById(R.id.cardDocente)
        tvLogin = findViewById(R.id.tvLogin)
    }

    private fun setupListeners() {
        cardAlumno.setOnClickListener {
            startActivity(Intent(this, RegisterAlumnoActivity::class.java))
        }

        cardApoderado.setOnClickListener {
            startActivity(Intent(this, RegisterApoderadoActivity::class.java))
        }

        cardDocente.setOnClickListener {
            startActivity(Intent(this, RegisterDocenteActivity::class.java))
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}