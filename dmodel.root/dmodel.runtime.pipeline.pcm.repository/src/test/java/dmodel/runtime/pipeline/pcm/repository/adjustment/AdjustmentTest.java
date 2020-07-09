package dmodel.runtime.pipeline.pcm.repository.adjustment;

import java.util.stream.Collectors;

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

import dmodel.base.core.facade.IPCMQueryFacade;
import dmodel.base.shared.ModelUtil;
import dmodel.runtime.pipeline.AbstractPipelineTestBase;
import dmodel.runtime.pipeline.pcm.repository.AbstractRepositoryTransformationTestBase;
import dmodel.runtime.pipeline.pcm.repository.adjustment.impl.TreeScalingRepositoryDerivationAdjuster;

@RunWith(SpringRunner.class)
@Import(AbstractRepositoryTransformationTestBase.RepositoryTransformationTestConfiguration.class)
public class AdjustmentTest extends AbstractPipelineTestBase {
	@Autowired
	private TreeScalingRepositoryDerivationAdjuster adjuster;

	@Autowired
	private IPCMQueryFacade pcm;

	@Before
	public void loadModels() {
		setSpecific(null, null, null);

		setPcm(ModelUtil.readFromFile("test-data/models/teastore.repository", Repository.class),
				ModelUtil.readFromFile("test-data/models/teastore.system", System.class),
				ModelUtil.readFromFile("test-data/models/teastore.resourceenvironment", ResourceEnvironment.class),
				ModelUtil.readFromFile("test-data/models/teastore.allocation", Allocation.class), null);

		this.reloadVsum();
	}

	@Test
	public void adjustmentTest() {
		adjuster.prepareAdjustments(pcm, null, Sets.newHashSet(), Sets.newHashSet());
		adjuster.prepareAdjustments(pcm, null,
				pcm.getRepository().getServiceCallGraph().getNodes().stream().map(n -> n.getSeff().getId())
						.collect(Collectors.toSet()),
				pcm.getRepository().getServiceCallGraph().getNodes().stream().map(n -> n.getSeff().getId())
						.collect(Collectors.toSet()));
	}

}
