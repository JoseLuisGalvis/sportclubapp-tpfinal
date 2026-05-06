package com.sportclub.app.ui.stripe

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.sportclub.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StripePaymentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAYMENT_URL = "payment_url"
        const val EXTRA_SESSION_ID  = "session_id"
        const val EXTRA_TYPE        = "type"
        const val EXTRA_ENTITY_ID   = "entity_id"
        const val RESULT_PAID       = 100
        const val RESULT_CANCELLED  = 101
    }

    private val viewModel: StripePaymentViewModel by viewModels()

    private lateinit var webView:     WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEstado:    TextView
    private lateinit var btnCerrar:   MaterialButton

    private var popupWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stripe_payment)
        supportActionBar?.title = "Pago con Stripe"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        setupWebView()
        observeUiState()
        observeEventos()

        val sessionId  = intent.getStringExtra(EXTRA_SESSION_ID)  ?: ""
        val entityType = intent.getStringExtra(EXTRA_TYPE)         ?: ""
        val entityId   = intent.getIntExtra(EXTRA_ENTITY_ID, 0)
        val paymentUrl = intent.getStringExtra(EXTRA_PAYMENT_URL)  ?: ""

        if (paymentUrl.isNotBlank()) {
            webView.loadUrl(paymentUrl)
            viewModel.iniciar(sessionId, entityType, entityId)
        } else {
            tvEstado.text        = "Error: URL de pago no disponible"
            btnCerrar.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        popupWebView?.destroy()
        popupWebView = null
        webView.destroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun bindViews() {
        webView     = findViewById(R.id.webViewStripe)
        progressBar = findViewById(R.id.progressBar)
        tvEstado    = findViewById(R.id.tvEstadoPago)
        btnCerrar   = findViewById(R.id.btnCerrar)
        btnCerrar.setOnClickListener { finish() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled                     = true
            domStorageEnabled                     = true
            loadWithOverviewMode                  = true
            useWideViewPort                       = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }

        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean,
                resultMsg: android.os.Message
            ): Boolean {
                val popup = WebView(this@StripePaymentActivity).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView, request: WebResourceRequest
                        ): Boolean {
                            val url = request.url.toString()
                            return when {
                                url.startsWith("https://sportclub.app/payment/success") -> {
                                    viewModel.onDeepLinkExito(); true
                                }
                                url.startsWith("https://sportclub.app/payment/cancel") -> {
                                    viewModel.onDeepLinkCancelado(); true
                                }
                                else -> false
                            }
                        }
                    }
                }
                popupWebView = popup
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = popup
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView) {
                popupWebView?.destroy()
                popupWebView = null
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                return when {
                    url.startsWith("sportclub://payment/success") -> {
                        viewModel.onDeepLinkExito(); true
                    }
                    url.startsWith("sportclub://payment/cancel") -> {
                        viewModel.onDeepLinkCancelado(); true
                    }
                    url.startsWith("http") || url.startsWith("https") -> false
                    else -> {
                        runCatching {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        true
                    }
                }
            }

            override fun onPageStarted(
                view: WebView?, url: String?,
                favicon: android.graphics.Bitmap?
            ) = viewModel.onWebPageStarted()

            override fun onPageFinished(view: WebView?, url: String?) =
                viewModel.onWebPageFinished()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                progressBar.visibility = if (state.webPageLoading) View.VISIBLE else View.GONE
                tvEstado.text          = state.estadoTexto
                btnCerrar.visibility   = if (state.mostrarCerrar) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeEventos() {
        lifecycleScope.launch {
            viewModel.eventos.collect { evento ->
                when (evento) {
                    is StripePaymentEvent.PagoExitoso   -> mostrarDialogoPagoExitoso()
                    is StripePaymentEvent.PagoCancelado -> mostrarDialogoPagoCancelado()
                    is StripePaymentEvent.Error         -> mostrarDialogoError(evento.mensaje)
                }
            }
        }
    }

    private fun mostrarDialogoPagoExitoso() {
        AlertDialog.Builder(this)
            .setTitle("¡Pago completado!")
            .setMessage("Tu pago fue procesado exitosamente.\nYa podés disfrutar de todos los beneficios del club.")
            .setPositiveButton("Aceptar") { _, _ -> setResult(RESULT_PAID); finish() }
            .setCancelable(false)
            .show()
    }

    private fun mostrarDialogoPagoCancelado() {
        AlertDialog.Builder(this)
            .setTitle("Pago cancelado")
            .setMessage("El pago fue cancelado.\nPodés intentarlo nuevamente desde tu perfil.")
            .setPositiveButton("Cerrar") { _, _ -> setResult(RESULT_CANCELLED); finish() }
            .show()
    }

    private fun mostrarDialogoError(mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle("Error de pago")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar") { _, _ -> finish() }
            .show()
    }
}