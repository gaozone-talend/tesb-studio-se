package org.talend.camel.designer.util

import scala.collection.JavaConversions._
import org.eclipse.emf.common.util.EList
import org.talend.core.GlobalServiceRegister
import org.talend.core.PluginChecker
import org.talend.core.model.properties.ProcessItem
import org.talend.core.ui.IJobletProviderService
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType
import org.talend.designer.core.model.utils.emf.talendfile.NodeType
import org.talend.repository.utils.EmfModelUtils

object CamelDesignerUtil {

	var service: IJobletProviderService = null
	if (PluginChecker.isJobLetPluginLoaded()) {
		service = GlobalServiceRegister.getDefault().getService(classOf[IJobletProviderService])
			.asInstanceOf[IJobletProviderService]
	}

	def checkRouteInputExistInJob(pi: ProcessItem): Boolean = {
		if (pi == null) false
		val process = pi.getProcess()
		recursiveCheckRouteInputExistInProcess(process)
	}

	def checkRouteInputExistInJoblet(jobletProcess: ProcessType): Boolean = {

		recursiveCheckRouteInputExistInProcess(jobletProcess)
	}

	/**
	 * Recursive check tRouteInput exists in process.
	 *
	 * @return true, if exists
	 */
	private def recursiveCheckRouteInputExistInProcess(process: ProcessType): Boolean = {
		if (process == null) false
		val nodes = process.getNode()
		nodes.toList.exists(e => {
			if (!e.isInstanceOf[NodeType]) false
			var nt = e.asInstanceOf[NodeType]
			if (!EmfModelUtils.isComponentActive(nt) && !EmfModelUtils.computeCheckElementValue("ACTIVATE", nt)) false
			var componentName = nt.getComponentName()
			if ("tRouteInput" == componentName) true
			if (service != null) {
				var process = service.getJobletProcess(nt)
				recursiveCheckRouteInputExistInProcess(process)
			}
			false
		})
	}
}