package dmodel.runtime.pipeline.pcm.system.sub;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import dmodel.base.core.IPcmModelProvider;
import dmodel.base.core.facade.IRuntimeEnvironmentQueryFacade;
import dmodel.base.shared.ModelUtil;
import dmodel.base.shared.structure.Tree;
import dmodel.designtime.monitoring.records.PCMContextRecord;
import dmodel.designtime.monitoring.records.ServiceCallRecord;
import dmodel.designtime.monitoring.util.MonitoringDataUtil;
import dmodel.runtime.pipeline.pcm.system.AbstractBaseSystemTransformationTest;
import dmodel.runtime.pipeline.pcm.system.RuntimeSystemTransformation;
import kieker.analysis.exception.AnalysisConfigurationException;

@RunWith(SpringRunner.class)
@Import(AbstractBaseSystemTransformationTest.SystemTransformationTestConfiguration.class)
public class TeastoreSystemTransformationTest extends AbstractBaseSystemTransformationTest {

	@Autowired
	private IPcmModelProvider pcm;

	@Autowired
	private RuntimeSystemTransformation transformation;

	@Autowired
	private IRuntimeEnvironmentQueryFacade remQuery;

	@Override
	protected void loadModels() {
		setSpecific(null, null, null);
		setPcm(ModelUtil.readFromFile("test-resources/models/teastore.repository", Repository.class),
				ModelUtil.readFromFile("test-resources/models/teastore.system",
						org.palladiosimulator.pcm.system.System.class),
				ModelUtil.readFromFile("test-resources/models/teastore.resourceenvironment", ResourceEnvironment.class),
				ModelUtil.readFromFile("test-resources/models/teastore.allocation", Allocation.class), null);
	}

	@Before
	@Override
	public void initHostMapping() {
		loadModels();
		super.reloadVsum();

		remQuery.createResourceContainer("f1039ae1e5c650f0228043ad4158babf", "VirtualizedTeastoreContainer");
	}

	@Test
	public void teastoreDerivationTest() throws IllegalStateException, AnalysisConfigurationException {
		List<PCMContextRecord> records = MonitoringDataUtil.getMonitoringDataFromFiles(
				"test-resources/monitoring/kieker-20200706-121111-12748299597224-UTC--KIEKER");

		List<Tree<ServiceCallRecord>> trees = MonitoringDataUtil
				.buildServiceCallTree(records.stream().filter(r -> r instanceof ServiceCallRecord)
						.map(ServiceCallRecord.class::cast).collect(Collectors.toList()));

		transformation.transformSystem(trees);

		ModelUtil.saveToFile(pcm.getSystem(), "result.system");
		ModelUtil.saveToFile(pcm.getAllocation(), "result.allocation");
	}

}
