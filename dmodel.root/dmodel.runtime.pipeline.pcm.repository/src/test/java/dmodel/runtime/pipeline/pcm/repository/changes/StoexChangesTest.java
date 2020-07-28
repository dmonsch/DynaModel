package dmodel.runtime.pipeline.pcm.repository.changes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.pcm.headless.api.util.ModelUtil;

import dmodel.base.shared.pcm.util.PCMUtils;
import dmodel.runtime.pipeline.pcm.repository.RepositoryStoexChanges;

public class StoexChangesTest {

	@BeforeClass
	public static void initPCM() {
		PCMUtils.loadPCMModels();
	}

	@Test
	public void testStoexChanges() {
		RepositoryStoexChanges changes = new RepositoryStoexChanges();

		Repository teastoreRepository = ModelUtil.readFromFile("test-data/models/teastore.repository",
				Repository.class);
		assertNotEquals(teastoreRepository, null);

		changes.put("__RGH0K1oEeqcvaJ1A892dQ", PCMUtils.createRandomVariableFromString("5 * 5"));
		changes.putConstant("_vHbbYDVgEeqPG_FgW3bi6Q",
				PCMUtils.createRandomVariableFromString("DoublePMF[(1;0.5)(2;0.5)]"));

		changes.apply(teastoreRepository);

		assertEquals(PCMUtils.getElementById(teastoreRepository, InternalAction.class, "__RGH0K1oEeqcvaJ1A892dQ")
				.getResourceDemand_Action().get(0).getSpecification_ParametericResourceDemand().getSpecification(),
				"5 * 5");
		assertEquals(PCMUtils.getElementById(teastoreRepository, ResourceDemandingSEFF.class, "_vHbbYDVgEeqPG_FgW3bi6Q")
				.getSteps_Behaviour().size(), 4);

		changes.putConstant("_vHbbYDVgEeqPG_FgW3bi6Q",
				PCMUtils.createRandomVariableFromString("DoublePMF[(3;0.5)(4;0.5)]"));

		changes.apply(teastoreRepository);

		assertEquals(PCMUtils.getElementById(teastoreRepository, InternalAction.class, "__RGH0K1oEeqcvaJ1A892dQ")
				.getResourceDemand_Action().get(0).getSpecification_ParametericResourceDemand().getSpecification(),
				"5 * 5");
		assertEquals(PCMUtils.getElementById(teastoreRepository, ResourceDemandingSEFF.class, "_vHbbYDVgEeqPG_FgW3bi6Q")
				.getSteps_Behaviour().size(), 4);
	}

}
