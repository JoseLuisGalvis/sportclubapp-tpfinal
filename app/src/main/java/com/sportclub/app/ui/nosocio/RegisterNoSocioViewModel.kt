package com.sportclub.app.ui.nosocio

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportclub.app.domain.usecase.RegistrarNoSocioUseCase
import com.sportclub.app.utils.FotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RegisterNoSocioUiState(
    val fechaVisitaMillis:    Long       = System.currentTimeMillis(),
    val fotoVisitaBytes:      ByteArray? = null,
    val fotoPreviewBmp:       Bitmap?    = null,
    val isLoading:            Boolean    = false,
    val btnGuardarHabilitado: Boolean    = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegisterNoSocioUiState) return false
        return fechaVisitaMillis     == other.fechaVisitaMillis &&
                fotoVisitaBytes  contentEquals other.fotoVisitaBytes &&
                fotoPreviewBmp       === other.fotoPreviewBmp &&
                isLoading            == other.isLoading &&
                btnGuardarHabilitado == other.btnGuardarHabilitado
    }
    override fun hashCode(): Int {
        var r = fechaVisitaMillis.hashCode()
        r = 31 * r + (fotoVisitaBytes?.contentHashCode() ?: 0)
        r = 31 * r + (fotoPreviewBmp?.hashCode() ?: 0)
        r = 31 * r + isLoading.hashCode()
        return r
    }
}

sealed class RegisterNoSocioEvent {
    data class Success(val nombre: String, val dni: String, val fechaVisita: String) : RegisterNoSocioEvent()
    data class ReadyForPayment(val idNoSocio: Int, val paymentUrl: String, val sessionId: String) : RegisterNoSocioEvent()
    data class ShowError(val message: String) : RegisterNoSocioEvent()
}

@HiltViewModel
class RegisterNoSocioViewModel @Inject constructor(
    private val registrarNoSocioUseCase: RegistrarNoSocioUseCase
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())

    private val _uiState = MutableStateFlow(RegisterNoSocioUiState())
    val uiState: StateFlow<RegisterNoSocioUiState> = _uiState.asStateFlow()

    private val _eventos = MutableSharedFlow<RegisterNoSocioEvent>()
    val eventos: SharedFlow<RegisterNoSocioEvent> = _eventos.asSharedFlow()

    fun onFechaSeleccionada(millis: Long) =
        _uiState.update { it.copy(fechaVisitaMillis = millis) }

    fun procesarFotoDesdeUri(fotoManager: FotoManager, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes    = fotoManager.procesarDesdeUri(uri) ?: return@runCatching null
                    val stream   = fotoManager.context.contentResolver.openInputStream(uri)
                    val bmp      = android.graphics.BitmapFactory.decodeStream(stream)
                    val escalado = fotoManager.escalar(bmp, 400)
                    Pair(bytes, escalado)
                }.getOrNull()
            }
            if (result != null) {
                _uiState.update { it.copy(
                    fotoVisitaBytes = result.first,
                    fotoPreviewBmp  = result.second,
                    isLoading       = false
                )}
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _eventos.emit(RegisterNoSocioEvent.ShowError("Error al cargar la imagen"))
            }
        }
    }

    fun procesarFotoDesdeCamara(fotoManager: FotoManager) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val archivo  = fotoManager.archivoCamaraTemp ?: return@runCatching null
                    val bytes    = fotoManager.procesarDesdeArchivo() ?: return@runCatching null
                    val bmp      = android.graphics.BitmapFactory.decodeFile(archivo.absolutePath)
                    val escalado = fotoManager.escalar(bmp, 400)
                    Pair(bytes, escalado)
                }.getOrNull()
            }
            if (result != null) {
                _uiState.update { it.copy(
                    fotoVisitaBytes = result.first,
                    fotoPreviewBmp  = result.second,
                    isLoading       = false
                )}
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _eventos.emit(RegisterNoSocioEvent.ShowError("Error al procesar la foto"))
            }
        }
    }

    fun registrar(
        nombre: String, apellido: String, dni: String,
        telefono: String?, email: String?,
        username: String, password: String, confirmPassword: String,
        habilitado: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, btnGuardarHabilitado = false) }
            val state  = _uiState.value
            val output = registrarNoSocioUseCase(
                nombre, apellido, dni, telefono, email,
                username, password, confirmPassword,
                habilitado, state.fechaVisitaMillis
            )
            when (output) {
                is RegistrarNoSocioUseCase.Output.ReadyForPayment ->
                    _eventos.emit(RegisterNoSocioEvent.ReadyForPayment(
                        output.idNoSocio,
                        output.session.paymentUrl,
                        output.session.sessionId
                    ))
                is RegistrarNoSocioUseCase.Output.RegistradoSinPago -> {
                    val fechaStr = dateFormatter.format(
                        Instant.ofEpochMilli(output.fechaVisita)
                    )
                    _eventos.emit(RegisterNoSocioEvent.Success(
                        output.nombre, output.dni, fechaStr
                    ))
                }
                is RegistrarNoSocioUseCase.Output.Failure ->
                    _eventos.emit(RegisterNoSocioEvent.ShowError(output.error.message))
            }
            _uiState.update { it.copy(isLoading = false, btnGuardarHabilitado = true) }
        }
    }
}