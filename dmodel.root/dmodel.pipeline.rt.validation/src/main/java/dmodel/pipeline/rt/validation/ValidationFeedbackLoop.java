package dmodel.pipeline.rt.validation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dmodel.pipeline.rt.validation.contracts.IValidationListener;
import dmodel.pipeline.rt.validation.data.ValidationData;
import dmodel.pipeline.rt.validation.data.ValidationState;
import dmodel.pipeline.shared.config.DModelConfigurationContainer;
import dmodel.pipeline.shared.monitoring.MonitoringDataContainer;
import dmodel.pipeline.shared.pcm.InMemoryPCM;

@Component
public class ValidationFeedbackLoop implements IValidationProcessor {
	private ValidationState currentState;

	private List<IValidationListener> listeners;

	private ValidationData currentData;

	@Autowired
	private DModelConfigurationContainer config;

	public ValidationFeedbackLoop() {
		this.currentState = ValidationState.INIT;
	}

	@Override
	public void input(InMemoryPCM instance, MonitoringDataContainer monitoringData) {
	}

	@Override
	public ValidationState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerValidationListener(IValidationListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void unregisterValidationListener(IValidationListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public ValidationData getData() {
		return currentData;
	}

	private void flushState(ValidationState state) {
		listeners.forEach(l -> l.stateChanged(state));
	}
}