package dmodel.runtime.pipelinepcm.usagemodel.transformation;

import java.util.List;
import java.util.stream.Collectors;

import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dmodel.base.core.config.ConfigurationContainer;
import dmodel.base.core.facade.IPCMQueryFacade;
import dmodel.base.shared.structure.Tree;
import dmodel.designtime.monitoring.records.ServiceCallRecord;
import dmodel.runtime.pipeline.validation.data.ValidationData;
import dmodel.runtime.pipelinepcm.usagemodel.IUsageDataExtractor;
import dmodel.runtime.pipelinepcm.usagemodel.tree.TreeBranchExtractor;
import lombok.extern.java.Log;

@Log
@Service
public class UsageDataDerivation implements InitializingBean {
	private IUsageDataExtractor treeExtractor;

	@Autowired
	private ConfigurationContainer configuration;

	public UsageDataDerivation() {
	}

	public List<UsageScenario> deriveUsageData(List<Tree<ServiceCallRecord>> callTrees, IPCMQueryFacade pcm,
			ValidationData validation) {
		// 1. derive usage data
		List<UsageScenario> data = this.treeExtractor.extract(callTrees, pcm.getRepository(), pcm.getSystem());
		data = data.stream()
				.filter(dt -> dt.getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour().size() > 2)
				.collect(Collectors.toList());

		log.info("Extracted " + data.size() + " usage scenario(s).");

		// 2. deduct
		return data;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.treeExtractor = new TreeBranchExtractor(configuration);
	}

}
