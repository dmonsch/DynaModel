package dmodel.runtime.pipeline.pcm.repository.teastore;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Sets;

import dmodel.base.core.facade.IRuntimeEnvironmentQueryFacade;
import dmodel.base.shared.ModelUtil;
import dmodel.designtime.monitoring.records.PCMContextRecord;
import dmodel.designtime.monitoring.util.MonitoringDataUtil;
import dmodel.runtime.pipeline.AbstractPipelineTestBase;
import dmodel.runtime.pipeline.blackboard.RuntimePipelineBlackboard;
import dmodel.runtime.pipeline.data.PCMPartionedMonitoringData;
import dmodel.runtime.pipeline.data.PartitionedMonitoringData;
import dmodel.runtime.pipeline.pcm.repository.AbstractRepositoryTransformationTestBase;
import dmodel.runtime.pipeline.pcm.repository.RepositoryDerivation;
import dmodel.runtime.pipeline.pcm.repository.RepositoryStoexChanges;
import dmodel.runtime.pipeline.validation.data.ValidationData;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(SpringRunner.class)
@Import(AbstractRepositoryTransformationTestBase.RepositoryTransformationTestConfiguration.class)
public class TeastoreRepositoryTransformationTest extends AbstractPipelineTestBase {

	@Autowired
	private RepositoryDerivation transformation;

	@Autowired
	private RuntimePipelineBlackboard blackboard;

	@Autowired
	private IRuntimeEnvironmentQueryFacade remQuery;

	@Before
	public void loadModels() {
		setSpecific(null, null, null);

		setPcm(ModelUtil.readFromFile("test-data/models/teastore.repository", Repository.class),
				ModelUtil.readFromFile("test-data/models/teastore.system", System.class),
				ModelUtil.readFromFile("test-data/models/teastore.resourceenvironment", ResourceEnvironment.class),
				ModelUtil.readFromFile("test-data/models/teastore.allocation", Allocation.class), null);

		this.reloadVsum();
		remQuery.createResourceContainer("f1039ae1e5c650f0228043ad4158babf", "VirtualizedTeastoreContainer");
	}

	@Test
	public void test() throws IllegalStateException, AnalysisConfigurationException {
		List<PCMContextRecord> dataset = MonitoringDataUtil.getMonitoringDataFromFiles("test-data/monitoring");
		PartitionedMonitoringData<PCMContextRecord> pcmData = new PCMPartionedMonitoringData(dataset, 0.1f);

		RepositoryStoexChanges changes = transformation.calibrateRepository(pcmData, blackboard.getPcmQuery(),
				new ValidationData(), Sets.newHashSet());

		assertTrue(changes.size() > 0);
	}

}
