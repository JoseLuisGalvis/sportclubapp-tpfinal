package com.sportclub.app.ui.loading

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sportclub.app.R
import com.sportclub.app.data.db.SportClubDatabase
import com.sportclub.app.ui.main.MainActivity
import com.sportclub.app.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadingActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_MS = 2500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val inicio = System.currentTimeMillis()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                SportClubDatabase.getDatabase(applicationContext)
            }
            val transcurrido = System.currentTimeMillis() - inicio
            val restante     = SPLASH_MS - transcurrido
            if (restante > 0) delay(restante)

            startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
            finish()
        }
    }
}