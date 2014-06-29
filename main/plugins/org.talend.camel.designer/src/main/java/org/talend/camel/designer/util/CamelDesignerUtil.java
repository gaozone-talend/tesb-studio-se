package org.talend.camel.designer.util;

import java.util.List;

import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.ui.IJobletProviderService;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.repository.utils.EmfModelUtils;

public class CamelDesignerUtil {

	private static IJobletProviderService service = null;

	static {
		if (PluginChecker.isJobLetPluginLoaded()) {
			service = (IJobletProviderService) GlobalServiceRegister.getDefault().getService(
					IJobletProviderService.class);
		}
	}

	public static boolean checkRouteInputExistInJob(ProcessItem pi) {
		if (pi == null) {
			return false;
		}
		return recursiveCheckRouteInputExistInProcess(pi.getProcess());
	}

	public static boolean checkRouteInputExistInJoblet(ProcessType jobletProcess) {

		return recursiveCheckRouteInputExistInProcess(jobletProcess);
	}

	private static boolean recursiveCheckRouteInputExistInProcess(ProcessType process) {
		if (process == null) {
			return false;
		}
		List<?> node = process.getNode();
		return node.stream()
				.filter(n -> n instanceof NodeType)
				.map(n -> (NodeType) n)
				.anyMatch(n -> {
					if (!EmfModelUtils.isComponentActive(n) && !EmfModelUtils.computeCheckElementValue("ACTIVATE", n)) {
						return false;
					}
					if ("tRouteInput".equals(n.getComponentName())) {
						return true;
					} else if (service != null) {
						ProcessType subProcess = service.getJobletProcess(n);
						return recursiveCheckRouteInputExistInProcess(subProcess);
					}
					return false;
				});

	}
}
