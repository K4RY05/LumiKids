package com.example.lumikids

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var etPass: EditText
    private lateinit var etPass2: EditText

    private lateinit var ivTogglePass: ImageButton
    private lateinit var ivTogglePass2: ImageButton

    private var isPasswordVisible = false
    private var isPasswordVisible2 = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etPass = findViewById(R.id.etPass)
        etPass2 = findViewById(R.id.etPass2)

        ivTogglePass = findViewById(R.id.ivTogglePass)
        ivTogglePass2 = findViewById(R.id.ivTogglePass2)

        ivTogglePass.setOnClickListener {
            togglePassword()
        }

        ivTogglePass2.setOnClickListener {
            togglePassword2()
        }
    }

    private fun togglePassword() {
        if (isPasswordVisible) {
            etPass.transformationMethod =
                PasswordTransformationMethod.getInstance()
            ivTogglePass.setImageResource(R.drawable.ic_eye_closed)
        } else {
            etPass.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            ivTogglePass.setImageResource(R.drawable.ic_eye_open)
        }

        etPass.setSelection(etPass.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun togglePassword2() {
        if (isPasswordVisible2) {
            etPass2.transformationMethod =
                PasswordTransformationMethod.getInstance()
            ivTogglePass2.setImageResource(R.drawable.ic_eye_closed)
        } else {
            etPass2.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            ivTogglePass2.setImageResource(R.drawable.ic_eye_open)
        }

        etPass2.setSelection(etPass2.text.length)
        isPasswordVisible2 = !isPasswordVisible2
    }
}
