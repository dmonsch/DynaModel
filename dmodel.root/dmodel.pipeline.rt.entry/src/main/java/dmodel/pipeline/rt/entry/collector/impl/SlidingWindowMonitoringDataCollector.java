package dmodel.pipeline.rt.entry.collector.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dmodel.pipeline.rt.entry.collector.IMonitoringDataCollector;
import dmodel.pipeline.rt.entry.core.IterativeRuntimePipeline;
import dmodel.pipeline.shared.config.DModelConfigurationContainer;
import dmodel.pipeline.shared.config.MonitoringDataEntryConfiguration;
import kieker.common.record.IMonitoringRecord;

@Component
public class SlidingWindowMonitoringDataCollector implements IMonitoringDataCollector, InitializingBean {

	private MonitoringDataEntryConfiguration config;

	@Autowired
	private DModelConfigurationContainer parentConfig;

	@Autowired
	private IterativeRuntimePipeline pipeline;

	private ScheduledExecutorService execService;

	private SortedMap<Long, List<IMonitoringRecord>> recordMap;

	@Override
	public void collect(IMonitoringRecord record) {
		long time = System.currentTimeMillis();
		if (recordMap.containsKey(time)) {
			recordMap.get(time).add(record);
		} else {
			List<IMonitoringRecord> nList = new LinkedList<>();
			nList.add(record);
			recordMap.put(time, nList);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// extract config
		this.config = this.parentConfig.getEntry();

		// record map
		this.recordMap = new TreeMap<>();

		// we need 2 threads at max
		this.execService = Executors.newScheduledThreadPool(2);

		// register trigger
		this.execService.scheduleAtFixedRate(() -> execTrigger(), config.getSlidingWindowTrigger(),
				config.getSlidingWindowTrigger(), TimeUnit.SECONDS);
	}

	private void execTrigger() {
		long currentTime = System.currentTimeMillis();
		// get subset
		List<IMonitoringRecord> collected = this.recordMap
				.subMap(currentTime - config.getSlidingWindowSize() * 1000, currentTime).entrySet().stream()
				.map(e -> e.getValue()).flatMap(List::stream).collect(Collectors.toList());

		// pass it to the processing part
		pipeline.triggerPipeline(collected);

		// cut old
		cutRecordMap(currentTime);
	}

	private void cutRecordMap(long currentTime) {
		recordMap.headMap(currentTime - config.getSlidingWindowSize() * 1000).clear();
	}

}
