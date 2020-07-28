package dmodel.runtime.pipeline.pcm.repository.adjustment.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.pcm.headless.shared.data.results.MeasuringPointType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import dmodel.base.core.facade.IPCMQueryFacade;
import dmodel.base.models.callgraph.ServiceCallGraph.ServiceCallGraph;
import dmodel.base.models.callgraph.ServiceCallGraph.ServiceCallGraphEdge;
import dmodel.base.shared.pcm.distribution.DoubleDistribution;
import dmodel.runtime.pipeline.pcm.repository.RepositoryStoexChanges;
import dmodel.runtime.pipeline.pcm.repository.adjustment.IRepositoryDerivationAdjustment;
import dmodel.runtime.pipeline.validation.data.ValidationData;
import dmodel.runtime.pipeline.validation.data.ValidationMetricValue;
import dmodel.runtime.pipeline.validation.data.metric.ValidationMetricType;
import dmodel.runtime.pipeline.validation.data.metric.value.DoubleMetricValue;
import lombok.extern.java.Log;

@Component
@Log
// TODO redo that
public class TreeScalingRepositoryDerivationAdjuster implements IRepositoryDerivationAdjustment {
	private static final double ADJUSTMENT_FACTOR = 0.1d;
	private static final double ADDITIVE_INCREASE = 0.02d;

	private static final double CONSTANT_DETERMINATION_N = 0.1d;

	private Map<String, Double> currentValidationAdjustment = Maps.newHashMap();
	private Map<String, Double> currentValidationAdjustmentGradient = Maps.newHashMap();

	@Override
	public RepositoryStoexChanges prepareAdjustments(IPCMQueryFacade pcm, ValidationData validation,
			Set<String> fineGrainedInstrumentedServices, Set<String> presentServices) {
		// 1. search for services to adjust
		ServiceCallGraph scg = pcm.getRepository().getServiceCallGraph();
		List<ResourceDemandingSEFF> servicesToAdjust = searchNodesToAdjust(scg, fineGrainedInstrumentedServices,
				presentServices);

		// 2. adjust the services
		Set<String> serviceIdsToAdjust = servicesToAdjust.stream().map(s -> s.getId()).collect(Collectors.toSet());

		RepositoryStoexChanges resultingChanges = this.constantDetermination(validation, serviceIdsToAdjust, pcm);
		this.prepareAdjustment(validation, serviceIdsToAdjust);

		return resultingChanges;
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

	private RepositoryStoexChanges constantDetermination(ValidationData validation, Set<String> servicesToAdjust,
			IPCMQueryFacade pcm) {
		RepositoryStoexChanges result = new RepositoryStoexChanges();
		if (validation == null || validation.isEmpty()) {
			return result;
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

					if (copyMonitoring.length > 0 && copyAnalysis.length > 0) {
						int nMonitoring = (int) Math.round(((double) copyMonitoring.length) * CONSTANT_DETERMINATION_N);
						int nAnalysis = (int) Math.round(((double) copyAnalysis.length) * CONSTANT_DETERMINATION_N);

						if (nMonitoring >= 2 && nAnalysis >= 2) {
							double[] lowestMonitoring = new double[nMonitoring];
							double[] lowestAnalysis = new double[nAnalysis];

							System.arraycopy(copyMonitoring, 0, lowestMonitoring, 0, nMonitoring);
							System.arraycopy(copyAnalysis, 0, lowestAnalysis, 0, nAnalysis);

							// do the investigation
							double avgAnalysis = StatUtils.mean(copyAnalysis);
							double avgMonitoring = StatUtils.mean(copyMonitoring);

							// significance test
							// TODO also adjust when lower?
							if (avgMonitoring != avgAnalysis
									&& new TTest().tTest(lowestMonitoring, lowestAnalysis, 0.05)) {
								log.info("Constant determination for service '" + point.getServiceId() + "'.");
								determineConstant(point.getServiceId(), monitoringDistribution, lowestMonitoring,
										lowestAnalysis, pcm, result);
							}
						}
					}
				}
			}
		});

		return result;
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
			double[] lowestAnalysis, IPCMQueryFacade pcm, RepositoryStoexChanges result) {
		// TODO calculation buggy?
		double avgAnalysis = StatUtils.mean(lowestAnalysis);

		DoubleDistribution adjustmentDistr = new DoubleDistribution(5);

		for (double monitoringValue : lowestMonitoring) {
			adjustmentDistr.put(monitoringValue - avgAnalysis);
		}

		// finally put it to the changes
		result.putConstant(serviceId, adjustmentDistr.toStoex());
	}

}
