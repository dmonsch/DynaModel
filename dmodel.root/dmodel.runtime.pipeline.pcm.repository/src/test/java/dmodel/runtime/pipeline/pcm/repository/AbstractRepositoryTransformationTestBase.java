package dmodel.runtime.pipeline.pcm.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import dmodel.runtime.pipeline.BasePipelineTestConfiguration;
import dmodel.runtime.pipeline.pcm.repository.adjustment.impl.TreeScalingRepositoryDerivationAdjuster;

public class AbstractRepositoryTransformationTestBase {

	@TestConfiguration
	public static class RepositoryTransformationTestConfiguration
			extends BasePipelineTestConfiguration.TestContextConfiguration {

		@Bean
		public RepositoryDerivation transformation() {
			return new RepositoryDerivation();
		}

		@Bean
		public TreeScalingRepositoryDerivationAdjuster adjuster() {
			return new TreeScalingRepositoryDerivationAdjuster();
		}

	}

}
