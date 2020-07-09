package dmodel.base.core.facade.pcm.impl;

import java.util.List;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.palladiosimulator.pcm.repository.OperationInterface;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.api.util.PCMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.uka.ipd.sdq.identifier.Identifier;
import dmodel.base.core.IPcmModelProvider;
import dmodel.base.core.facade.pcm.IRepositoryQueryFacade;
import dmodel.base.models.callgraph.ServiceCallGraph.ServiceCallGraph;
import dmodel.base.models.callgraph.ServiceCallGraph.ServiceCallGraphFactory;
import dmodel.base.shared.pcm.util.repository.PCMRepositoryUtil;

/**
 * Implementation of the repository facade for accessing the underlying
 * repository model. Uses caching between ID string and elements to speedup
 * accesses.
 * 
 * @author David Monschein
 *
 */
@Component
public class RepositoryQueryFacadeImpl implements IRepositoryQueryFacade {
	/**
	 * Provider of the underlying models.
	 */
	@Autowired
	private IPcmModelProvider pcmModelProvider;

	/**
	 * Cache that maps ID string to the corresponding elements in the repository
	 * model.
	 */
	private Cache<String, Identifier> elementIdCache = new Cache2kBuilder<String, Identifier>() {
	}.eternal(true).build();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset(boolean hard) {
		elementIdCache.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Identifier> T getElementById(String id, Class<T> type) {
		if (id == null) {
			return null;
		}

		if (elementIdCache.containsKey(id)) {
			return type.cast(elementIdCache.get(id));
		} else {
			T element = PCMUtil.getElementById(pcmModelProvider.getRepository(), type, id);
			if (element != null) {
				elementIdCache.put(id, element);
			}
			return element;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<RepositoryComponent> getComponentsProvidingInterface(OperationInterface iface) {
		return PCMRepositoryUtil.getComponentsProvidingInterface(pcmModelProvider.getRepository(), iface);
	}

	@Override
	public ServiceCallGraph getServiceCallGraph() {
		List<ResourceDemandingSEFF> seffs = ModelUtil.getObjects(pcmModelProvider.getRepository(),
				ResourceDemandingSEFF.class);
		ServiceCallGraph output = ServiceCallGraphFactory.eINSTANCE.createServiceCallGraph();

		seffs.forEach(seff -> {
			output.addNode(seff, null);
		});

		seffs.forEach(seff -> {
			List<ExternalCallAction> externalCalls = ModelUtil.getObjects(seff, ExternalCallAction.class);
			externalCalls.forEach(ext -> {
				OperationInterface iface = ext.getCalledService_ExternalService().getInterface__OperationSignature();
				getComponentsProvidingInterface(iface).forEach(c -> {
					ResourceDemandingSEFF correspondingSeff = ModelUtil.getObjects(c, ResourceDemandingSEFF.class)
							.stream().filter(iseff -> {
								return iseff.getDescribedService__SEFF().getId()
										.equals(ext.getCalledService_ExternalService().getId());
							}).findFirst().orElse(null);

					if (correspondingSeff != null) {
						output.incrementEdge(seff, correspondingSeff, null, null, ext);
					}
				});
			});
		});

		return output;
	}

}
