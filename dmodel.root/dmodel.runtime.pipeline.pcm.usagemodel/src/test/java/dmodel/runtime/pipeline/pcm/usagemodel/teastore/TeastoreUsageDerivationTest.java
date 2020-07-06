package dmodel.runtime.pipeline.pcm.usagemodel.teastore;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import dmodel.base.core.IPcmModelProvider;
import dmodel.base.shared.ModelUtil;
import dmodel.base.shared.structure.Tree;
import dmodel.designtime.monitoring.records.PCMContextRecord;
import dmodel.designtime.monitoring.records.ServiceCallRecord;
import dmodel.designtime.monitoring.util.MonitoringDataUtil;
import dmodel.runtime.pipeline.AbstractPipelineTestBase;
import dmodel.runtime.pipeline.blackboard.RuntimePipelineBlackboard;
import dmodel.runtime.pipeline.pcm.usagemodel.AbstractBaseUsageModelDerivationTest;
import dmodel.runtime.pipelinepcm.usagemodel.transformation.UsageDataDerivation;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(SpringRunner.class)
@Import(AbstractBaseUsageModelDerivationTest.UsageTransformationTestConfiguration.class)
public class TeastoreUsageDerivationTest extends AbstractPipelineTestBase {

	@Autowired
	protected UsageDataDerivation derivation;

	@Autowired
	protected RuntimePipelineBlackboard blackboard;

	@Autowired
	protected IPcmModelProvider pcm;

	@Test
	public void teastoreDerivationTest() throws IllegalStateException, AnalysisConfigurationException {

		List<PCMContextRecord> records = MonitoringDataUtil.getMonitoringDataFromFiles(
				"test-data/teastore/monitoring/kieker-20200703-123839-5918591706114-UTC--KIEKER");

		List<Tree<ServiceCallRecord>> trees = MonitoringDataUtil
				.buildServiceCallTree(records.stream().filter(r -> r instanceof ServiceCallRecord)
						.map(ServiceCallRecord.class::cast).collect(Collectors.toList()));

		this.deriveUsageData(trees);
	}

	@Before
	public void loadModelsAndVsum() {
		loadModels();
		super.reloadVsum();
	}

	protected void loadModels() {
		super.setSpecific(null, null, null);
		super.setPcm(
				ModelUtil.readFromFile(new File("test-data/teastore/models/teastore.repository").getAbsolutePath(),
						Repository.class),
				ModelUtil.readFromFile(new File("test-data/teastore/models/teastore.system").getAbsolutePath(),
						System.class),
				ModelUtil.readFromFile(
						new File("test-data/teastore/models/teastore.resourceenvironment").getAbsolutePath(),
						ResourceEnvironment.class),
				ModelUtil.readFromFile(new File("test-data/teastore/models/teastore.allocation").getAbsolutePath(),
						Allocation.class),
				null);
	}

	protected void deriveUsageData(List<Tree<ServiceCallRecord>> records) {
		List<UsageScenario> scenarios = derivation.deriveUsageData(records, blackboard.getPcmQuery(), null);
		pcm.getUsage().getUsageScenario_UsageModel().clear();
		pcm.getUsage().getUsageScenario_UsageModel().addAll(scenarios);

		ModelUtil.saveToFile(pcm.getUsage(), "result.usagemodel");
	}

}
