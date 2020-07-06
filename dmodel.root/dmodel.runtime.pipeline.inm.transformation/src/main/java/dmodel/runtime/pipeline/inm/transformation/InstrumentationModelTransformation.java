package dmodel.runtime.pipeline.inm.transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;
import org.pcm.headless.shared.data.results.MeasuringPointType;
import org.pcm.headless.shared.data.results.PlainMeasuringPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import dmodel.base.core.config.ConfigurationContainer;
import dmodel.base.core.config.PredicateRuleConfiguration;
import dmodel.base.core.evaluation.ExecutionMeasuringPoint;
import dmodel.base.models.inmodel.InstrumentationMetamodel.ServiceInstrumentationPoint;
import dmodel.base.shared.pipeline.PortIDs;
import dmodel.runtime.pipeline.AbstractIterativePipelinePart;
import dmodel.runtime.pipeline.annotation.InputPort;
import dmodel.runtime.pipeline.annotation.InputPorts;
import dmodel.runtime.pipeline.blackboard.RuntimePipelineBlackboard;
import dmodel.runtime.pipeline.inm.transformation.predicate.ValidationPredicate;
import dmodel.runtime.pipeline.inm.transformation.predicate.config.ELogicalOperator;
import dmodel.runtime.pipeline.inm.transformation.predicate.config.ENumericalComparator;
import dmodel.runtime.pipeline.inm.transformation.predicate.impl.DoubleMetricValidationPredicate;
import dmodel.runtime.pipeline.inm.transformation.predicate.impl.StackedValidationPredicate;
import dmodel.runtime.pipeline.validation.data.ValidationData;
import dmodel.runtime.pipeline.validation.data.ValidationMetricValue;
import dmodel.runtime.pipeline.validation.data.ValidationPoint;
import dmodel.runtime.pipeline.validation.data.metric.ValidationMetricType;
import dmodel.runtime.pipeline.validation.data.metric.value.DoubleMetricValue;
import lombok.extern.java.Log;

@Log
@Service
public class InstrumentationModelTransformation extends AbstractIterativePipelinePart<RuntimePipelineBlackboard> {
	private static final Map<String, ValidationMetricType> predicateMetricMapping = new HashMap<String, ValidationMetricType>() {
		/**
		 * Generated Serial UID.
		 */
		private static final long serialVersionUID = 1634402137017012232L;
		{
			put("kstest", ValidationMetricType.KS_TEST);
			put("avg_rel", ValidationMetricType.AVG_DISTANCE_REL);
			put("avg_absolute", ValidationMetricType.AVG_DISTANCE_ABS);
			put("wasserstein", ValidationMetricType.WASSERSTEIN);
			put("median_absolute", ValidationMetricType.MEDIAN_DISTANCE);
		}
	};

	public InstrumentationModelTransformation() {
		super(ExecutionMeasuringPoint.T_INSTRUMENTATION_MODEL, null);
	}

	@Autowired
	private ConfigurationContainer configuration;

	@InputPorts({ @InputPort(PortIDs.T_VAL_IMM) })
	public void adjustInstrumentationModel(ValidationData validation) {
		if (validation.isEmpty()) {
			super.trackStart();
			super.trackEnd();
			return;
		}

		super.trackStart();

		Set<String> deInstrumentServices = Sets.newHashSet();
		Set<String> instrumentServices = Sets.newHashSet();

		ValidationPredicate validationPredicate = toJavaPredicate(configuration.getValidationPredicates());
		ValidationPoint test = ValidationPoint.builder().build();
		ValidationMetricValue val = new DoubleMetricValue(0.1, ValidationMetricType.KS_TEST);
		ValidationMetricValue val2 = new DoubleMetricValue(0.1, ValidationMetricType.AVG_DISTANCE_REL);

		test.getMetricValues().add(val);
		test.getMetricValues().add(val2);

		validation.getValidationPoints().forEach(validationPoint -> {
			if (isServiceMeasuringPoint(validationPoint.getMeasuringPoint())) {
				String serviceId = validationPoint.getServiceId();
				if (serviceId != null) {
					boolean fulfilled = validationPredicate == null || validationPredicate.satisfied(validationPoint);

					if (fulfilled) {
						deInstrumentServices.add(serviceId);
					} else {
						instrumentServices.add(serviceId);
					}
				}
			}
		});

		// if we have a component on multiple hosts
		// and it is not accurate on a single one
		deInstrumentServices.removeAll(instrumentServices);

		// check whether target service is accurate
		if (configuration.getVfl().getTargetServiceId() != null
				&& !configuration.getVfl().getTargetServiceId().isEmpty()) {
			if (deInstrumentServices.contains(configuration.getVfl().getTargetServiceId())) {
				deInstrumentServices.addAll(instrumentServices);
				instrumentServices.clear();
			}
		}

		// perform changes
		deInstrumentServices.forEach(deInstr -> {
			log.info("Enable coarse grained monitoring for service '" + deInstr + "'.");
			changeInstrumentationModel(deInstr, false);
		});

		instrumentServices.forEach(instr -> {
			log.info("Enable fine grained monitoring for service '" + instr + "'.");
			changeInstrumentationModel(instr, true);
		});

		super.trackEnd();
	}

	private void changeInstrumentationModel(String deInstr, boolean b) {
		ServiceEffectSpecification seff = getBlackboard().getPcmQuery().getRepository().getServiceById(deInstr);
		Optional<ServiceInstrumentationPoint> instrPoint = getBlackboard().getVsumQuery()
				.getCorrespondingInstrumentationPoint(seff);
		if (instrPoint.isPresent()) {
			instrPoint.get().getActionInstrumentationPoints().forEach(actionPoint -> actionPoint.setActive(b));
		}
	}

	private boolean isServiceMeasuringPoint(PlainMeasuringPoint measuringPoint) {
		if (measuringPoint.getType() == MeasuringPointType.ASSEMBLY_OPERATION
				|| measuringPoint.getType() == MeasuringPointType.ENTRY_LEVEL_CALL) {
			return true;
		}
		return false;
	}

	private ValidationPredicate toJavaPredicate(PredicateRuleConfiguration config) {
		if (config.getCondition() != null) {
			// it is stacked
			ValidationPredicate[] predicates = new ValidationPredicate[config.getRules().size()];

			for (int i = 0; i < config.getRules().size(); i++) {
				predicates[i] = toJavaPredicate(config.getRules().get(i));
			}

			return new StackedValidationPredicate(ELogicalOperator.fromString(config.getCondition()), predicates);
		} else {
			return buildSimpleJavaPredicate(config);
		}
	}

	private ValidationPredicate buildSimpleJavaPredicate(PredicateRuleConfiguration config) {
		ENumericalComparator comparator = ENumericalComparator.fromString(config.getOperator());
		if (comparator != null && predicateMetricMapping.containsKey(config.getId())) {
			return new DoubleMetricValidationPredicate(predicateMetricMapping.get(config.getId()), comparator,
					config.getValue());
		} else {
			log.warning("Metric of type '" + config.getId() + "' or comparator '" + comparator.getName()
					+ "' is not supported yet for the predicate generation.");
		}

		return null;
	}

}
