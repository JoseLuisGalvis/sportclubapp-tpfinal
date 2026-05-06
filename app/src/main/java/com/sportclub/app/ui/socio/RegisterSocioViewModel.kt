package com.sportclub.app.ui.socio

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportclub.app.domain.usecase.RegistrarSocioUseCase
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
import javax.inject.Inject

data class RegisterSocioUiState(
    val fechaAltaMillis:      Long       = System.currentTimeMillis(),
    val fotoCarnetBytes:      ByteArray? = null,
    val fotoPreviewBmp:       Bitmap?    = null,
    val isLoading:            Boolean    = false,
    val btnGuardarHabilitado: Boolean    = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegisterSocioUiState) return false
        return fechaAltaMillis       == other.fechaAltaMillis &&
                fotoCarnetBytes contentEquals other.fotoCarnetBytes &&
                fotoPreviewBmp       === other.fotoPreviewBmp &&
                isLoading            == other.isLoading &&
                btnGuardarHabilitado == other.btnGuardarHabilitado
    }
    override fun hashCode(): Int {
        var r = fechaAltaMillis.hashCode()
        r = 31 * r + (fotoCarnetBytes?.contentHashCode() ?: 0)
        r = 31 * r + (fotoPreviewBmp?.hashCode() ?: 0)
        r = 31 * r + isLoading.hashCode()
        return r
    }
}

sealed class RegisterSocioEvent {
    data class Success(val nombre: String, val dni: String, val carnet: String) : RegisterSocioEvent()
    data class ReadyForPayment(val nroSocio: Int, val paymentUrl: String, val sessionId: String) : RegisterSocioEvent()
    data class ShowError(val message: String) : RegisterSocioEvent()
}

@HiltViewModel
class RegisterSocioViewModel @Inject constructor(
    private val registrarSocioUseCase: RegistrarSocioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterSocioUiState())
    val uiState: StateFlow<RegisterSocioUiState> = _uiState.asStateFlow()

    private val _eventos = MutableSharedFlow<RegisterSocioEvent>()
    val eventos: SharedFlow<RegisterSocioEvent> = _eventos.asSharedFlow()

    fun onFechaSeleccionada(millis: Long) =
        _uiState.update { it.copy(fechaAltaMillis = millis) }

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
                    fotoCarnetBytes = result.first,
                    fotoPreviewBmp  = result.second,
                    isLoading       = false
                )}
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _eventos.emit(RegisterSocioEvent.ShowError("Error al cargar la imagen"))
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
                    fotoCarnetBytes = result.first,
                    fotoPreviewBmp  = result.second,
                    isLoading       = false
                )}
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _eventos.emit(RegisterSocioEvent.ShowError("Error al procesar la foto"))
            }
        }
    }

    fun registrar(
        nombre: String, apellido: String, dni: String,
        telefono: String?, email: String?,
        username: String, password: String, confirmPassword: String,
        habilitado: Boolean, aptoFisico: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, btnGuardarHabilitado = false) }
            val state  = _uiState.value
            val output = registrarSocioUseCase(
                nombre, apellido, dni, telefono, email,
                username, password, confirmPassword,
                habilitado, aptoFisico,
                state.fechaAltaMillis, state.fotoCarnetBytes
            )
            when (output) {
                is RegistrarSocioUseCase.Output.ReadyForPayment ->
                    _eventos.emit(RegisterSocioEvent.ReadyForPayment(
                        output.nroSocio,
                        output.session.paymentUrl,
                        output.session.sessionId
                    ))
                is RegistrarSocioUseCase.Output.RegistradoOffline ->
                    _eventos.emit(RegisterSocioEvent.Success(
                        output.nombre,
                        output.dni,
                        output.carnet
                    ))
                is RegistrarSocioUseCase.Output.Failure ->
                    _eventos.emit(RegisterSocioEvent.ShowError(output.error.message))
            }
            _uiState.update { it.copy(isLoading = false, btnGuardarHabilitado = true) }
        }
    }
}