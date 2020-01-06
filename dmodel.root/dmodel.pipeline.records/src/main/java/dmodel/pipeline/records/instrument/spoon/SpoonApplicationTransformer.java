package dmodel.pipeline.records.instrument.spoon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.springframework.stereotype.Service;

import InstrumentationMetamodel.ActionInstrumentationPoint;
import InstrumentationMetamodel.InstrumentationModel;
import InstrumentationMetamodel.ServiceInstrumentationPoint;
import dmodel.pipeline.records.instrument.ApplicationProject;
import dmodel.pipeline.records.instrument.IApplicationInstrumenter;
import dmodel.pipeline.records.instrument.InstrumentationMetadata;
import dmodel.pipeline.records.instrument.spoon.instrument.impl.SpoonMethodInstrumenter;
import spoon.Launcher;
import spoon.reflect.declaration.CtMethod;

@Service
public class SpoonApplicationTransformer implements IApplicationInstrumenter {
	private final SpoonMethodInstrumenter methodTransformer;

	public SpoonApplicationTransformer() {
		methodTransformer = new SpoonMethodInstrumenter();
	}

	@Override
	public boolean instrumentApplication(Launcher model, InstrumentationMetadata metadata,
			SpoonCorrespondence spoonCorr) {

		// instrument the services coarse grained
		for (Entry<CtMethod<?>, ResourceDemandingSEFF> serviceEntry : spoonCorr.getServiceMappingEntries()) {
			Optional<ServiceInstrumentationPoint> instrPoint = this.resolveInstrumentationPoint(metadata.getProbes(),
					serviceEntry.getValue());
			if (instrPoint.isPresent() && instrPoint.get().isActive()) {
				// instrument it
				methodTransformer.instrument(model, serviceEntry.getKey(), instrPoint.get());

				for (ActionInstrumentationPoint innerInstrPoint : instrPoint.get().getActionInstrumentationPoints()) {
					// TODO
				}
			}
		}

		return true;
	}

	@Override
	public Launcher prepareModifiableModel(ApplicationProject project, ApplicationProject agentConfig,
			String outputPath) {
		// prepare the project
		File nProject = prepareOutputProject(project, outputPath, true);
		if (nProject == null) {
			return null;
		}

		// add the agent
		prepareOutputProject(agentConfig, outputPath, false);

		// load the model
		ApplicationProject copyProject = new ApplicationProject();
		copyProject.setRootPath(nProject.getAbsolutePath() + File.separator);
		copyProject.setSourceFolders(new ArrayList<String>(project.getSourceFolders()));
		return this.createModel(copyProject);
	}

	public Launcher createModel(ApplicationProject project) {
		Launcher spoon = new Launcher();

		// load all sources
		for (String srcFolder : project.getSourceFolders()) {
			spoon.addInputResource(project.getRootPath() + srcFolder);
		}
		spoon.buildModel();

		return spoon;
	}

	private Optional<ServiceInstrumentationPoint> resolveInstrumentationPoint(InstrumentationModel model,
			ResourceDemandingSEFF seff) {
		return model.getPoints().stream().filter(p -> p.getService().getId().equals(seff.getId())).findFirst();
	}

	private File prepareOutputProject(ApplicationProject project, String outputPath, boolean deletePre) {
		File outputFolder = new File(outputPath);
		for (String sourceFolder : project.getSourceFolders()) {
			File fullSourceFolder = new File(project.getRootPath(), sourceFolder);
			File fullTargetFolder = new File(outputFolder, sourceFolder);

			if (deletePre && fullTargetFolder.exists()) {
				// we need to clear it
				try {
					FileUtils.deleteDirectory(fullTargetFolder);
				} catch (IOException e) {
					return null;
				}
			}

			if (fullTargetFolder.mkdirs() || fullTargetFolder.exists()) {
				try {
					FileUtils.copyDirectory(fullSourceFolder, fullTargetFolder);
				} catch (IOException e) {
					return null;
				}
			} else {
				return null;
			}
		}

		return outputFolder;
	}

}
