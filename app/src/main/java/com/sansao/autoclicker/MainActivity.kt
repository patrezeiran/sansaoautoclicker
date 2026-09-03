package com.sansao.autoclicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnAccessibility: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnStartFloating: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnOverlay = findViewById(R.id.btnOverlay)
        btnStartFloating = findViewById(R.id.btnStartFloating)

        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnStartFloating.setOnClickListener {
            Toast.makeText(this, "Pronto! Na próxima etapa subiremos o painel flutuante.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val hasAccessibility = AutoClickerService.isRunning || isAccessibilityServiceEnabled(this, AutoClickerService::class.java)
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        btnAccessibility.text = if (hasAccessibility) "1. Acessibilidade (Ativada ✓)" else "1. Ativar Acessibilidade"
        btnOverlay.text = if (hasOverlay) "2. Sobreposição (Ativada ✓)" else "2. Ativar Sobreposição"

        btnStartFloating.isEnabled = hasAccessibility && hasOverlay
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expected = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any { it.equals(expected, ignoreCase = true) }
    }
}
