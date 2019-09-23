package dmodel.pipeline.rt.rest.core.config;

import java.io.File;
import java.io.IOException;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dmodel.pipeline.rt.rest.data.config.ModelPathContainer;
import dmodel.pipeline.rt.rest.data.config.ModelPathResponse;
import dmodel.pipeline.shared.ModelUtil;
import dmodel.pipeline.shared.config.DModelConfigurationContainer;
import dmodel.pipeline.shared.config.ModelConfiguration;

@RestController
public class ConfigModelsRestController {
	private static final String[] DEFAULT_PATH_ALLOCATION = new String[] { "models", "temp_allocation.allocation" };
	private static final String[] DEFAULT_PATH_REPOSITORY = new String[] { "models", "temp_repository.repository" };
	private static final String[] DEFAULT_PATH_RESENV = new String[] { "models", "temp_resenv.resourcenevironment" };
	private static final String[] DEFAULT_PATH_USAGE = new String[] { "models", "temp_usage.usagemodel" };
	private static final String[] DEFAULT_PATH_SYSTEM = new String[] { "models", "temp_system.system" };

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private DModelConfigurationContainer config;

	@GetMapping("/config/models/get")
	public String getModelConfig() {
		try {
			return objectMapper.writeValueAsString(ModelPathContainer.from(config));
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	@PostMapping("/config/models/save")
	public String saveModels(@RequestParam String models) {
		try {
			ModelPathContainer req = objectMapper.readValue(models, ModelPathContainer.class);
			ModelPathResponse val = validateModelPaths(req);

			ModelConfiguration into = config.getModels();
			enrichWithProjectConfig(req);
			if (val.isAlloc()) {
				into.setAllocationPath(req.getAlloc());
			}
			if (val.isRepo()) {
				into.setRepositoryPath(req.getRepo());
			}
			if (val.isRes()) {
				into.setEnvPath(req.getRes());
			}
			if (val.isSys()) {
				into.setSystemPath(req.getSys());
			}
			if (val.isUsage()) {
				into.setUsagePath(req.getUsage());
			}
			return config.syncWithFilesystem() ? "{\"success\" : true}" : "{\"success\" : false}";
		} catch (IOException e) {
			e.printStackTrace();
			return "{\"success\" : false}";
		}
	}

	@PostMapping("/config/models/validate")
	public String validateModels(@RequestParam String models) {
		try {
			ModelPathContainer req = objectMapper.readValue(models, ModelPathContainer.class);
			return objectMapper.writeValueAsString(validateModelPaths(req));
		} catch (IOException e) {
			return null;
		}
	}

	private void enrichWithProjectConfig(ModelPathContainer req) {
		if (config.getProject() != null && config.getProject().getRootPath() != null) {
			File basePath = new File(config.getProject().getRootPath());

			if (req.getAlloc().length() == 0) {
				req.setAlloc(buildDefaultPath(basePath, DEFAULT_PATH_ALLOCATION));
			}
			if (req.getRepo().length() == 0) {
				req.setRepo(buildDefaultPath(basePath, DEFAULT_PATH_REPOSITORY));
			}
			if (req.getRes().length() == 0) {
				req.setRes(buildDefaultPath(basePath, DEFAULT_PATH_RESENV));
			}
			if (req.getSys().length() == 0) {
				req.setSys(buildDefaultPath(basePath, DEFAULT_PATH_SYSTEM));
			}
			if (req.getUsage().length() == 0) {
				req.setUsage(buildDefaultPath(basePath, DEFAULT_PATH_USAGE));
			}
		}
	}

	private String buildDefaultPath(File basePath, String[] exts) {
		return new File(new File(basePath, exts[0]), exts[1]).getAbsolutePath();
	}

	private ModelPathResponse validateModelPaths(ModelPathContainer req) {
		ModelPathResponse resp = new ModelPathResponse();
		resp.setAlloc(ModelUtil.validateModelPath(req.getAlloc(), Allocation.class));
		resp.setRepo(ModelUtil.validateModelPath(req.getRepo(), Repository.class));
		resp.setRes(ModelUtil.validateModelPath(req.getRes(), ResourceEnvironment.class));
		resp.setSys(ModelUtil.validateModelPath(req.getSys(), System.class));
		resp.setUsage(ModelUtil.validateModelPath(req.getUsage(), UsageModel.class));
		return resp;
	}

}
