package com.sportclub.app.ui.stripe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportclub.app.domain.usecase.VerificarPagoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StripePaymentUiState(
    val webPageLoading: Boolean = false,
    val estadoTexto:    String  = "Procesando pago…",
    val mostrarCerrar:  Boolean = false,
    val pollingActivo:  Boolean = false
)

sealed class StripePaymentEvent {
    object PagoExitoso   : StripePaymentEvent()
    object PagoCancelado : StripePaymentEvent()
    data class Error(val mensaje: String) : StripePaymentEvent()
}

@HiltViewModel
class StripePaymentViewModel @Inject constructor(
    private val verificarPagoUseCase: VerificarPagoUseCase
) : ViewModel() {

    companion object {
        private const val POLLING_INTERVAL_MS   = 3_000L
        private const val POLLING_DELAY_INICIAL = 5_000L
        private const val MAX_INTENTOS          = 60
    }

    private val _uiState = MutableStateFlow(StripePaymentUiState())
    val uiState: StateFlow<StripePaymentUiState> = _uiState.asStateFlow()

    private val _eventos = MutableSharedFlow<StripePaymentEvent>()
    val eventos: SharedFlow<StripePaymentEvent> = _eventos.asSharedFlow()

    private var sessionId:  String = ""
    private var entityType: String = ""
    private var entityId:   Int    = 0
    private var pollingJob: Job?   = null

    fun iniciar(sessionId: String, entityType: String, entityId: Int) {
        this.sessionId  = sessionId
        this.entityType = entityType
        this.entityId   = entityId
        iniciarPolling()
    }

    private fun iniciarPolling() {
        if (sessionId.isBlank()) return

        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(pollingActivo = true) }
            delay(POLLING_DELAY_INICIAL)

            repeat(MAX_INTENTOS) { intento ->
                val output = verificarPagoUseCase(sessionId, entityType, entityId)

                when (output) {
                    is VerificarPagoUseCase.Output.Confirmado -> {
                        _uiState.update { it.copy(pollingActivo = false) }
                        _eventos.emit(StripePaymentEvent.PagoExitoso)
                        return@launch
                    }
                    is VerificarPagoUseCase.Output.Fallido -> {
                        // Error de red — continuar polling
                    }
                    is VerificarPagoUseCase.Output.Pendiente -> Unit
                }

                if (intento == MAX_INTENTOS - 1) {
                    _uiState.update {
                        it.copy(
                            estadoTexto   = "Tiempo de espera agotado.\nSi realizaste el pago verificá tu email.",
                            mostrarCerrar = true,
                            pollingActivo = false
                        )
                    }
                } else {
                    delay(POLLING_INTERVAL_MS)
                }
            }
        }
    }

    fun onDeepLinkExito() {
        pollingJob?.cancel()
        viewModelScope.launch {
            val output = verificarPagoUseCase(sessionId, entityType, entityId)
            _uiState.update { it.copy(pollingActivo = false) }
            _eventos.emit(StripePaymentEvent.PagoExitoso)
        }
    }

    fun onDeepLinkCancelado() {
        pollingJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(pollingActivo = false, mostrarCerrar = true) }
            _eventos.emit(StripePaymentEvent.PagoCancelado)
        }
    }

    fun onWebPageStarted()  = _uiState.update { it.copy(webPageLoading = true)  }
    fun onWebPageFinished() = _uiState.update { it.copy(webPageLoading = false) }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}