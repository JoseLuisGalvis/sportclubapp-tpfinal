package com.sportclub.app.ui.main

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.sportclub.app.R
import com.sportclub.app.databinding.ActivityMainBinding
import com.sportclub.app.ui.auth.login.AdminLoginActivity
import com.sportclub.app.ui.nosocio.RegisterNoSocioActivity
import com.sportclub.app.ui.socio.RegisterSocioActivity
import com.sportclub.app.utils.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actualizarUI()
        setupClickListeners()
    }

    private fun actualizarUI() {
        val isDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        binding.ivLogo.setImageResource(
            if (isDark) R.drawable.natacion_white else R.drawable.natacion
        )
        binding.btnToggleTheme.text = if (isDark) "☀" else "🌙"
    }

    private fun setupClickListeners() {
        binding.btnToggleTheme.setOnClickListener {
            ThemeManager.toggleTheme(this)
            val isDark = ThemeManager.isDarkMode(this)
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                else        AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        binding.btnAcceder.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }
        binding.btnRegistrarSocio.setOnClickListener {
            startActivity(Intent(this, RegisterSocioActivity::class.java))
        }
        binding.btnRegistrarNoSocio.setOnClickListener {
            startActivity(Intent(this, RegisterNoSocioActivity::class.java))
        }
        binding.btnInstagram.setOnClickListener { openUrl("https://www.instagram.com") }
        binding.btnLinkedin.setOnClickListener  { openUrl("https://www.linkedin.com") }
        binding.btnYoutube.setOnClickListener   { openUrl("https://www.youtube.com") }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu) = false

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}