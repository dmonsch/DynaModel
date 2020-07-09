package dmodel.runtime.pipeline.pcm.repository.adjustment.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.resourcetype.ProcessingResourceType;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.seff_performance.ParametricResourceDemand;
import org.palladiosimulator.pcm.seff.seff_performance.SeffPerformanceFactory;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.shared.data.results.MeasuringPointType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import dmodel.base.core.facade.IPCMQueryFacade;
import dmodel.base.models.callgraph.ServiceCallGraph.ServiceCallGraph;
import dmodel.base.models.callgraph.ServiceCallGraph.ServiceCallGraphEdge;
import dmodel.base.shared.pcm.distribution.DoubleDistribution;
import dmodel.base.shared.pcm.util.PCMUtils;
import dmodel.runtime.pipeline.pcm.repository.adjustment.IRepositoryDerivationAdjustment;
import dmodel.runtime.pipeline.validation.data.ValidationData;
import dmodel.runtime.pipeline.validation.data.ValidationMetricValue;
import dmodel.runtime.pipeline.validation.data.metric.ValidationMetricType;
import dmodel.runtime.pipeline.validation.data.metric.value.DoubleMetricValue;
import lombok.extern.java.Log;

@Component
@Log
public class TreeScalingRepositoryDerivationAdjuster implements IRepositoryDerivationAdjustment {
	private static final double ADJUSTMENT_FACTOR = 0.1d;
	private static final double ADDITIVE_INCREASE = 0.02d;

	private static final String INTERNAL_ACTION_ADJUSTMENT_KEY = "-CD-ADJUST";
	private static final String CPU_RESOURCE_ID = "_oro4gG3fEdy4YaaT-RYrLQ";

	private static final int CONSTANT_DETERMINATION_N = 10;

	private Map<String, Double> currentValidationAdjustment = Maps.newHashMap();
	private Map<String, Double> currentValidationAdjustmentGradient = Maps.newHashMap();

	@Override
	public void prepareAdjustments(IPCMQueryFacade pcm, ValidationData validation,
			Set<String> fineGrainedInstrumentedServices, Set<String> presentServices) {
		// 1. search for services to adjust
		ServiceCallGraph scg = pcm.getRepository().getServiceCallGraph();
		List<ResourceDemandingSEFF> servicesToAdjust = searchNodesToAdjust(scg, fineGrainedInstrumentedServices,
				presentServices);

		// 2. adjust the services
		Set<String> serviceIdsToAdjust = servicesToAdjust.stream().map(s -> s.getId()).collect(Collectors.toSet());
		this.constantDetermination(validation, serviceIdsToAdjust, pcm);
		this.prepareAdjustment(validation, serviceIdsToAdjust);
	}

	@Override
	public Map<String, Double> getAdjustments() {
		return currentValidationAdjustment;
	}

	private List<ResourceDemandingSEFF> searchNodesToAdjust(ServiceCallGraph scg, Set<String> fineGrained,
			Set<String> present) {
		return scg.getNodes().stream().filter(n -> {
			List<ServiceCallGraphEdge> outgoingEdges = scg.getOutgoingEdges().get(n);
			return outgoingEdges == null || outgoingEdges.size() == 0 || outgoingEdges.stream().allMatch(nn -> {
				return !present.contains(nn.getTo().getSeff().getId())
						|| !fineGrained.contains(nn.getTo().getSeff().getId());
			});
		}).map(n -> n.getSeff()).collect(Collectors.toList());
	}

	private void constantDetermination(ValidationData validation, Set<String> servicesToAdjust, IPCMQueryFacade pcm) {
		if (validation == null || validation.isEmpty()) {
			return;
		}

		validation.getValidationPoints().forEach(point -> {
			MeasuringPointType type = point.getMeasuringPoint().getType();
			if (type == MeasuringPointType.ASSEMBLY_OPERATION || type == MeasuringPointType.ENTRY_LEVEL_CALL) {
				if (servicesToAdjust.contains(point.getServiceId())) {
					double[] monitoringDistribution = point.getMonitoringDistribution().yAxis();
					double[] analysisDistribution = point.getAnalysisDistribution().yAxis();

					double[] copyMonitoring = new double[monitoringDistribution.length];
					double[] copyAnalysis = new double[analysisDistribution.length];

					System.arraycopy(monitoringDistribution, 0, copyMonitoring, 0, monitoringDistribution.length);
					System.arraycopy(analysisDistribution, 0, copyAnalysis, 0, analysisDistribution.length);
					Arrays.sort(copyMonitoring);
					Arrays.sort(copyAnalysis);

					if (copyMonitoring.length > CONSTANT_DETERMINATION_N
							&& copyAnalysis.length > CONSTANT_DETERMINATION_N) {
						double[] lowestMonitoring = new double[CONSTANT_DETERMINATION_N];
						double[] lowestAnalysis = new double[CONSTANT_DETERMINATION_N];

						System.arraycopy(copyMonitoring, 0, lowestMonitoring, 0, CONSTANT_DETERMINATION_N);
						System.arraycopy(copyAnalysis, 0, lowestAnalysis, 0, CONSTANT_DETERMINATION_N);

						// do the investigation
						double avgAnalysis = StatUtils.mean(copyAnalysis);
						double avgMonitoring = StatUtils.mean(copyMonitoring);

						// significance test
						// TODO also adjust when lower?
						if (avgMonitoring != avgAnalysis && new TTest().tTest(lowestMonitoring, lowestAnalysis, 0.05)) {
							log.info("Constant determination for service '" + point.getServiceId() + "'.");
							determineConstant(point.getServiceId(), monitoringDistribution, lowestMonitoring,
									lowestAnalysis, pcm);
						}
					}
				}
			}
		});
	}

	private void prepareAdjustment(ValidationData validation, Set<String> servicesToAdjust) {
		if (validation == null || validation.isEmpty()) {
			return;
		}

		validation.getValidationPoints().forEach(point -> {
			MeasuringPointType type = point.getMeasuringPoint().getType();
			if (type == MeasuringPointType.ASSEMBLY_OPERATION || type == MeasuringPointType.ENTRY_LEVEL_CALL) {
				if (point.getServiceId() != null && point.getMonitoringDistribution() != null
						&& servicesToAdjust.contains(point.getServiceId())) {
					DoubleMetricValue valueRelDist = null;
					// absolute dist
					double valueAbsDist = StatUtils.mean(point.getMonitoringDistribution().yAxis())
							- StatUtils.mean(point.getAnalysisDistribution().yAxis());

					// check the metric
					for (ValidationMetricValue metric : point.getMetricValues()) {
						if (metric.type() == ValidationMetricType.AVG_DISTANCE_REL) {
							valueRelDist = ((DoubleMetricValue) metric);
						}
					}

					if (valueRelDist != null) {
						if (valueAbsDist > 0) {
							adjustService(point.getServiceId(), true);
						} else if (valueAbsDist < 0) {
							adjustService(point.getServiceId(), false);
						}
					}
				}
			}
		});
	}

	private void adjustService(String service, boolean scaleUp) {
		if (!currentValidationAdjustment.containsKey(service)) {
			currentValidationAdjustment.put(service, ADJUSTMENT_FACTOR * (scaleUp ? 1 : -1));
			currentValidationAdjustmentGradient.put(service, ADJUSTMENT_FACTOR);
		} else {
			double adjustmentBefore = currentValidationAdjustment.get(service);
			double currentGradient = currentValidationAdjustmentGradient.get(service);

			double adjustmentNow;
			if (scaleUp && adjustmentBefore < 0 || !scaleUp && adjustmentBefore > 0) {
				adjustmentNow = ADJUSTMENT_FACTOR * (scaleUp ? 1d : -1d);
			} else {
				adjustmentNow = currentGradient + ADDITIVE_INCREASE * Math.signum(currentGradient);
			}

			currentValidationAdjustment.put(service, adjustmentNow + adjustmentBefore);
			currentValidationAdjustmentGradient.put(service, adjustmentNow);
		}
	}

	private void determineConstant(String serviceId, double[] monitoringDistribution, double[] lowestMonitoring,
			double[] lowestAnalysis, IPCMQueryFacade pcm) {
		double avgAnalysis = StatUtils.mean(lowestAnalysis);

		DoubleDistribution adjustmentDistr = new DoubleDistribution(5);

		for (double monitoringValue : lowestMonitoring) {
			adjustmentDistr.put(monitoringValue - avgAnalysis);
		}

		// search for corresponding internal action
		InternalAction adjustmentIA = pcm.getRepository().getInternalAction(serviceId + INTERNAL_ACTION_ADJUSTMENT_KEY);
		if (adjustmentIA == null) {
			ResourceDemandingSEFF seff = pcm.getRepository().getServiceById(serviceId);
			createInternalAction(seff, serviceId + INTERNAL_ACTION_ADJUSTMENT_KEY, adjustmentDistr.toStoex());
		} else {
			adjustmentIA.getResourceDemand_Action().get(0)
					.setSpecification_ParametericResourceDemand(adjustmentDistr.toStoex());
		}
	}

	private InternalAction createInternalAction(ResourceDemandingSEFF seff, String id, PCMRandomVariable demand) {
		InternalAction nAction = SeffFactory.eINSTANCE.createInternalAction();
		nAction.setId(id);

		StartAction start = ModelUtil.getObjects(seff, StartAction.class).get(0);
		start.getSuccessor_AbstractAction().setPredecessor_AbstractAction(nAction);
		nAction.setSuccessor_AbstractAction(start.getSuccessor_AbstractAction());

		start.setSuccessor_AbstractAction(nAction);
		nAction.setPredecessor_AbstractAction(start);

		ParametricResourceDemand nDemand = SeffPerformanceFactory.eINSTANCE.createParametricResourceDemand();
		nDemand.setSpecification_ParametericResourceDemand(demand);
		nDemand.setRequiredResource_ParametricResourceDemand(PCMUtils.getElementById(
				PCMUtils.getDefaultResourceRepository(), ProcessingResourceType.class, CPU_RESOURCE_ID));
		nAction.getResourceDemand_Action().add(nDemand);

		return nAction;
	}

}
