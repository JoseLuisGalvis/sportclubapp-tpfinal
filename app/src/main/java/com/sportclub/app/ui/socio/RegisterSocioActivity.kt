package com.sportclub.app.ui.socio

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sportclub.app.databinding.ActivityRegisterSocioBinding
import com.sportclub.app.ui.stripe.StripePaymentActivity
import com.sportclub.app.utils.FotoManager
import com.sportclub.app.utils.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@AndroidEntryPoint
class RegisterSocioActivity : AppCompatActivity() {

    private lateinit var binding:     ActivityRegisterSocioBinding
    private lateinit var fotoManager: FotoManager

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())

    private val viewModel: RegisterSocioViewModel by viewModels()

    private val launcherCamara = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) viewModel.procesarFotoDesdeCamara(fotoManager)
    }

    private val launcherGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.procesarFotoDesdeUri(fotoManager, uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding     = ActivityRegisterSocioBinding.inflate(layoutInflater)
        fotoManager = FotoManager(this)
        setContentView(binding.root)
        supportActionBar?.title = "Registro de Socio"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupListeners()
        observeUiState()
        observeEventos()
    }

    private fun setupListeners() {
        binding.btnFechaAlta.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                viewModel.onFechaSeleccionada(cal.timeInMillis)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show()
        }
        binding.btnSeleccionarFoto.setOnClickListener { mostrarDialogoFoto() }
        binding.btnGuardar.setOnClickListener {
            viewModel.registrar(
                nombre          = binding.etNombre.text.toString().trim(),
                apellido        = binding.etApellido.text.toString().trim(),
                dni             = binding.etDni.text.toString().trim(),
                telefono        = binding.etTelefono.text.toString().trim().ifBlank { null },
                email           = binding.etEmail.text.toString().trim().ifBlank { null },
                username        = binding.etUsername.text.toString().trim(),
                password        = binding.etPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString(),
                habilitado      = binding.switchHabilitado.isChecked,
                aptoFisico      = binding.switchAptoFisico.isChecked
            )
        }
        binding.btnCancelar.setOnClickListener { finish() }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility =
                    if (state.isLoading) View.VISIBLE else View.GONE
                binding.btnGuardar.isEnabled = state.btnGuardarHabilitado
                binding.btnFechaAlta.text = dateFormatter.format(
                    Instant.ofEpochMilli(state.fechaAltaMillis)
                )
                state.fotoPreviewBmp?.let { bmp ->
                    binding.ivFotoPreview.setImageBitmap(bmp)
                    binding.ivFotoPreview.visibility = View.VISIBLE
                    binding.btnSeleccionarFoto.text  = "Cambiar foto"
                }
            }
        }
    }

    private fun observeEventos() {
        lifecycleScope.launch {
            viewModel.eventos.collect { evento ->
                when (evento) {
                    is RegisterSocioEvent.ShowError       -> toast(evento.message)
                    is RegisterSocioEvent.Success         -> mostrarDialogoExito(evento)
                    is RegisterSocioEvent.ReadyForPayment -> mostrarModalNroSocio(evento)
                }
            }
        }
    }

    private fun mostrarDialogoFoto() {
        AlertDialog.Builder(this)
            .setTitle("Foto para carnet")
            .setItems(arrayOf("Tomar foto con cámara", "Elegir de galería")) { _, which ->
                when (which) {
                    0 -> fotoManager.crearUriCamara("foto_carnet")
                        ?.let { launcherCamara.launch(it) }
                        ?: toast("No se pudo abrir la cámara")
                    1 -> launcherGaleria.launch("image/*")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoExito(evento: RegisterSocioEvent.Success) {
        AlertDialog.Builder(this)
            .setTitle("Socio registrado")
            .setMessage("Nombre: ${evento.nombre}\nDNI: ${evento.dni}\nCarnet: ${evento.carnet}")
            .setPositiveButton("Aceptar") { _, _ -> finish() }
            .show()
    }

    private fun mostrarModalNroSocio(evento: RegisterSocioEvent.ReadyForPayment) {
        AlertDialog.Builder(this)
            .setTitle("¡Registro exitoso!")
            .setMessage(
                "Tu número de socio es:\n\n  N° ${evento.nroSocio}\n\n" +
                        "Al presionar Aceptar se abrirá el módulo de pago."
            )
            .setPositiveButton("Aceptar") { _, _ ->
                startActivity(
                    Intent(this, StripePaymentActivity::class.java).apply {
                        putExtra(StripePaymentActivity.EXTRA_PAYMENT_URL, evento.paymentUrl)
                        putExtra(StripePaymentActivity.EXTRA_SESSION_ID,  evento.sessionId)
                        putExtra(StripePaymentActivity.EXTRA_TYPE,        "socio")
                        putExtra(StripePaymentActivity.EXTRA_ENTITY_ID,   evento.nroSocio)
                    }
                )
                finish()
            }
            .setNegativeButton("Pagar después") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}