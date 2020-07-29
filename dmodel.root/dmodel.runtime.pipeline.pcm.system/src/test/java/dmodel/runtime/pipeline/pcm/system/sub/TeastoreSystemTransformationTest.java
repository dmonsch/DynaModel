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

		remQuery.createResourceContainer("67f0132e7401d4390bc3a201463dd269", "0c5543ea567c");
		remQuery.createResourceContainer("29f3738d316c2db8b810651a11254811", "a05c8367ac1a");
		remQuery.createResourceContainer("46182745a0ff24d2d5c7f8f3c39357a4", "71a769a2a851");
		remQuery.createResourceContainer("5f64786520b6839ead17f4dec53116ae", "350b6af59ebe");
	}

	@Test
	public void teastoreDerivationTest() throws IllegalStateException, AnalysisConfigurationException {
		List<PCMContextRecord> records = MonitoringDataUtil.getMonitoringDataFromFiles(
				"test-resources/monitoring/kieker-20200729-112157-3327798188707-UTC--KIEKER");

		List<Tree<ServiceCallRecord>> trees = MonitoringDataUtil
				.buildServiceCallTree(records.stream().filter(r -> r instanceof ServiceCallRecord)
						.map(ServiceCallRecord.class::cast).collect(Collectors.toList()));

		transformation.transformSystem(trees);

		ModelUtil.saveToFile(pcm.getSystem(), "result.system");
		ModelUtil.saveToFile(pcm.getAllocation(), "result.allocation");
	}

}
