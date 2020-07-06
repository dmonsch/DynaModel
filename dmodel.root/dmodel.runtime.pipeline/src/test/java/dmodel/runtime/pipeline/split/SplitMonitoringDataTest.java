package dmodel.runtime.pipeline.split;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import dmodel.designtime.monitoring.records.PCMContextRecord;
import dmodel.designtime.monitoring.util.MonitoringDataUtil;
import dmodel.runtime.pipeline.data.PCMPartionedMonitoringData;
import kieker.analysis.exception.AnalysisConfigurationException;

public class SplitMonitoringDataTest {

	@Test
	public void splitTest() throws IllegalStateException, AnalysisConfigurationException {
		List<PCMContextRecord> records = MonitoringDataUtil
				.getMonitoringDataFromFiles("test-data/kieker-20200624-191224-36105003290429-UTC--KIEKER");

		PCMPartionedMonitoringData partitoned = new PCMPartionedMonitoringData(records, 0.1f);

		assertTrue(partitoned.getAllData().size() > 0);
		assertTrue(partitoned.getTrainingData().size() > 0);
		assertTrue(partitoned.getValidationData().size() > 0);

		System.out.println(partitoned.getTrainingData().size());
		System.out.println(partitoned.getValidationData().size());
		System.out.println(partitoned.getAllData().size());
	}

}
