package dev.panopt.autonomia.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.panopt.autonomia.AutonomiaRepository
import dev.panopt.autonomia.app.AppGraph
import dev.panopt.autonomia.domain.onboarding.OnboardingFlow
import dev.panopt.autonomia.domain.onboarding.OnboardingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado y acciones del onboarding de introducción. Reúne los dos hechos persistidos
 * (flag de completitud + paso en curso) y los resuelve a un [OnboardingState] vía
 * [OnboardingFlow]. No contiene reglas: solo orquesta lectura/persistencia.
 */
internal class OnboardingViewModel(
    private val repository: AutonomiaRepository,
) : ViewModel() {

    val onboardingState: StateFlow<OnboardingState> =
        combine(
            repository.isInitialConfigurationCompleteFlow(),
            repository.onboardingCurrentStepFlow(),
        ) { completed, stepName ->
            OnboardingFlow.resolve(completed, stepName)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = OnboardingFlow.resolve(
                completed = repository.isInitialConfigurationCompleteFlow().value,
                persistedStepName = repository.onboardingCurrentStepFlow().value,
            ),
        )

    fun advance() {
        val next = OnboardingFlow.next(onboardingState.value.currentStep)
        viewModelScope.launch { repository.setOnboardingCurrentStep(next.name) }
    }

    fun back() {
        val previous = OnboardingFlow.previous(onboardingState.value.currentStep)
        viewModelScope.launch { repository.setOnboardingCurrentStep(previous.name) }
    }

    fun complete() {
        viewModelScope.launch { repository.setInitialConfigurationComplete(true) }
    }

    class Factory(
        private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(AppGraph.autonomiaRepository(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
