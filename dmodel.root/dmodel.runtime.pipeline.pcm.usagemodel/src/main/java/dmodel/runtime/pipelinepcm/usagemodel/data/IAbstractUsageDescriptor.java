package dmodel.runtime.pipelinepcm.usagemodel.data;

import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;

public interface IAbstractUsageDescriptor extends IPCMAnalogue<AbstractUserAction> {
	public boolean matches(IAbstractUsageDescriptor other);

	public void merge(IAbstractUsageDescriptor other);
}
