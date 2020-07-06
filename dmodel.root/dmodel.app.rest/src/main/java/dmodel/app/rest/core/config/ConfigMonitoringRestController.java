package dmodel.app.rest.core.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dmodel.base.core.config.ConfigurationContainer;
import dmodel.base.core.config.PredicateRuleConfiguration;
import dmodel.base.shared.JsonUtil;

@RestController
public class ConfigMonitoringRestController {
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ConfigurationContainer configurationContainer;

	@PostMapping("/config/monitoring/predicate/save")
	public String savePredicate(@RequestParam String predicate) {
		try {
			PredicateRuleConfiguration parsedPredicate = objectMapper.readValue(predicate,
					PredicateRuleConfiguration.class);
			configurationContainer.setValidationPredicates(parsedPredicate);

			return JsonUtil.wrapAsObject("success", configurationContainer.syncWithFilesystem(false), false);
		} catch (IOException e) {
			return JsonUtil.wrapAsObject("success", false, false);
		}
	}

	@GetMapping("/config/monitoring/predicate/get")
	public String getPredicate() {
		try {
			return objectMapper.writeValueAsString(configurationContainer.getValidationPredicates());
		} catch (JsonProcessingException e) {
			return JsonUtil.wrapAsObject("success", false, false);
		}
	}

}
