package dmodel.runtime.pipeline.entry.receiver;

import java.util.List;
import java.util.stream.Collectors;

import dmodel.base.shared.structure.Tree;
import dmodel.base.shared.structure.Tree.TreeNode;
import dmodel.designtime.monitoring.records.PCMContextRecord;
import dmodel.designtime.monitoring.records.ResponseTimeRecord;
import dmodel.designtime.monitoring.records.ServiceCallRecord;
import dmodel.designtime.monitoring.util.MonitoringDataUtil;
import kieker.analysis.exception.AnalysisConfigurationException;

public class PlainServiceCallGraphVisualizer {

	public static void main(String[] args) throws IllegalStateException, AnalysisConfigurationException {
		List<PCMContextRecord> records = MonitoringDataUtil.getMonitoringDataFromFiles(
				"/Users/david/Desktop/Dynamic Approach/Implementation/git2/dModel/dmodel.root/dmodel.runtime.pipeline.entry/received/kieker-20200701-095729-3859005778663-UTC--KIEKER");

		List<Tree<ServiceCallRecord>> trees = MonitoringDataUtil
				.buildServiceCallTree(records.stream().filter(r -> r instanceof ServiceCallRecord)
						.map(ServiceCallRecord.class::cast).collect(Collectors.toList()));
		List<ResponseTimeRecord> responseTimes = records.stream().filter(r -> r instanceof ResponseTimeRecord)
				.map(ResponseTimeRecord.class::cast).collect(Collectors.toList());

		for (Tree<ServiceCallRecord> tree : trees) {
			visualizeTree(tree.getRoot(), responseTimes, 0);
		}
	}

	private static void visualizeTree(TreeNode<ServiceCallRecord> n, List<ResponseTimeRecord> records, int space) {
		String spacedString = " ".repeat(space);
		System.out.println(spacedString + "-" + dataToString(n.getData(), getCoverage(n, records)));
		for (TreeNode<ServiceCallRecord> node : n.getChildren()) {
			visualizeTree(node, records, space + 4);
		}
	}

	private static double getCoverage(TreeNode<ServiceCallRecord> data, List<ResponseTimeRecord> recs) {
		double durationWhole = data.getData().getExitTime() - data.getData().getEntryTime();
		double durationChilds = 0.0d;

		for (TreeNode<ServiceCallRecord> child : data.getChildren()) {
			durationChilds += (child.getData().getExitTime() - child.getData().getEntryTime());
		}

		for (ResponseTimeRecord rec : recs) {
			if (rec.getServiceExecutionId().equals(data.getData().getServiceExecutionId())) {
				durationChilds += rec.getStopTime() - rec.getStartTime();
			}
		}

		return (100d / durationWhole) * durationChilds;
	}

	private static String dataToString(ServiceCallRecord data, double coverage) {
		return "|" + data.getExternalCallId() + "| " + data.getServiceId() + " [" + data.getHostName() + "]" + " - "
				+ String.valueOf((data.getExitTime() - data.getEntryTime()) / 1000000L) + "ms" + "- {"
				+ data.getParameters() + "}" + " - Coverage: " + String.valueOf(Math.round(coverage * 100) / 100)
				+ " %";
	}

}
