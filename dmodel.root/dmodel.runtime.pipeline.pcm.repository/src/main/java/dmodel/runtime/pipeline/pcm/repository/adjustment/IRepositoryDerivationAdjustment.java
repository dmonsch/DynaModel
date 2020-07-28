package dmodel.runtime.pipeline.pcm.repository.adjustment;

import java.util.Map;
import java.util.Set;

import dmodel.base.core.facade.IPCMQueryFacade;
import dmodel.runtime.pipeline.pcm.repository.RepositoryStoexChanges;
import dmodel.runtime.pipeline.validation.data.ValidationData;

public interface IRepositoryDerivationAdjustment {

	public RepositoryStoexChanges prepareAdjustments(IPCMQueryFacade pcm, ValidationData validation,
			Set<String> fineGrainedInstrumentedServices, Set<String> presentServices);

	public Map<String, Double> getAdjustments();

}
