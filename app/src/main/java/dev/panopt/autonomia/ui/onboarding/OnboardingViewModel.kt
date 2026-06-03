package dev.panopt.autonomia.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.panopt.autonomia.AutonomiaRepository
import dev.panopt.autonomia.app.AppGraph
import dev.panopt.autonomia.domain.onboarding.OnboardingFlow
import dev.panopt.autonomia.domain.onboarding.OnboardingIntention
import dev.panopt.autonomia.domain.onboarding.OnboardingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado y acciones del onboarding de introducción. Reúne los tres hechos persistidos
 * (flag de completitud + paso en curso + intención) y los resuelve a un [OnboardingState]
 * vía [OnboardingFlow]. No contiene reglas: solo orquesta lectura/persistencia.
 */
internal class OnboardingViewModel(
    private val repository: AutonomiaRepository,
) : ViewModel() {

    val onboardingState: StateFlow<OnboardingState> =
        combine(
            repository.isInitialConfigurationCompleteFlow(),
            repository.onboardingCurrentStepFlow(),
            repository.onboardingIntentionFlow(),
        ) { completed, stepName, intentionName ->
            OnboardingFlow.resolve(
                completed = completed,
                persistedStepName = stepName,
                persistedIntention = intentionName,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = OnboardingFlow.resolve(
                completed = repository.isInitialConfigurationCompleteFlow().value,
                persistedStepName = repository.onboardingCurrentStepFlow().value,
                persistedIntention = repository.onboardingIntentionFlow().value,
            ),
        )

    fun advance() {
        val s = onboardingState.value
        val next = OnboardingFlow.next(s.currentStep, s.intention)
        viewModelScope.launch { repository.setOnboardingCurrentStep(next.name) }
    }

    fun back() {
        val s = onboardingState.value
        val previous = OnboardingFlow.previous(s.currentStep, s.intention)
        viewModelScope.launch { repository.setOnboardingCurrentStep(previous.name) }
    }

    fun complete() {
        viewModelScope.launch { repository.setInitialConfigurationComplete(true) }
    }

    /** Persiste la intención elegida por el usuario en prefs. */
    fun selectIntention(intention: OnboardingIntention) {
        viewModelScope.launch { repository.setOnboardingIntention(intention.name) }
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
