package dmodel.runtime.pipeline.pcm.repository;

import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dmodel.base.core.facade.IPCMQueryFacade;
import dmodel.base.core.facade.IRuntimeEnvironmentQueryFacade;
import dmodel.base.vsum.facade.ISpecificVsumFacade;
import dmodel.designtime.monitoring.records.PCMContextRecord;
import dmodel.designtime.monitoring.records.ServiceCallRecord;
import dmodel.runtime.pipeline.data.PartitionedMonitoringData;
import dmodel.runtime.pipeline.pcm.repository.adjustment.IRepositoryDerivationAdjustment;
import dmodel.runtime.pipeline.pcm.repository.branch.impl.BranchEstimationImpl;
import dmodel.runtime.pipeline.pcm.repository.core.ResourceDemandEstimatorAlternative;
import dmodel.runtime.pipeline.pcm.repository.loop.impl.LoopEstimationImpl;
import dmodel.runtime.pipeline.pcm.repository.model.IResourceDemandEstimator;
import dmodel.runtime.pipeline.validation.data.ValidationData;
import lombok.extern.java.Log;

/**
 * 
 * @author David Monschein
 *
 */
@Service
@Log
public class RepositoryDerivation {
	@Autowired
	private ISpecificVsumFacade mappingFacade;

	@Autowired
	private IRuntimeEnvironmentQueryFacade remQuery;

	@Autowired
	private IRepositoryDerivationAdjustment adjuster;

	private final LoopEstimationImpl loopEstimation;
	private final BranchEstimationImpl branchEstimation;

	public RepositoryDerivation() {
		this.loopEstimation = new LoopEstimationImpl();
		this.branchEstimation = new BranchEstimationImpl();
	}

	public RepositoryStoexChanges calibrateRepository(PartitionedMonitoringData<PCMContextRecord> data,
			IPCMQueryFacade pcm, ValidationData validation, Set<String> toPrepare) {
		try {
			Set<String> presentServices = data.getAllData().stream().filter(f -> f instanceof ServiceCallRecord)
					.map(ServiceCallRecord.class::cast).map(s -> s.getServiceId()).collect(Collectors.toSet());

			adjuster.prepareAdjustments(pcm, validation, toPrepare, presentServices);

			MonitoringDataSet monitoringDataSet = new MonitoringDataSet(data.getTrainingData(), mappingFacade, remQuery,
					pcm.getAllocation(), pcm.getRepository());

			// TODO integrate loop and branch estimation

			IResourceDemandEstimator estimation = new ResourceDemandEstimatorAlternative(pcm);
			estimation.prepare(monitoringDataSet);
			RepositoryStoexChanges result = estimation.derive(adjuster.getAdjustments());

			log.info("Finished calibration of internal actions.");
			log.info("Finished repository calibration.");

			return result;
		} catch (Exception e) {
			log.info("Calibration failed.");
			log.log(Level.INFO, "Calibrate Repository failed.", e);

			return null;
		}

	}

}
