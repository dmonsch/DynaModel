package dmodel.designtime.monitoring.performance;

import dmodel.designtime.monitoring.controller.ThreadMonitoringController;

public class MonitoringControllerPerformanceTest {

	public static void main(String[] args) {
		ThreadMonitoringController.getInstance().enterService("Test", MonitoringControllerPerformanceTest.class);

		for (int i = 0; i < 1000; i++) {
			ThreadMonitoringController.getInstance().enterService("test2", args);
			ThreadMonitoringController.getInstance().enterInternalAction("a", "ab");

			ThreadMonitoringController.getInstance().exitInternalAction("a", "ab");
			ThreadMonitoringController.getInstance().exitService("test2");
		}

		ThreadMonitoringController.getInstance().exitService("Test");
	}

}
