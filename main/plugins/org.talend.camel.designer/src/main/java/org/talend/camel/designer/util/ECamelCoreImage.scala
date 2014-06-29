package org.talend.camel.designer.util

import org.talend.commons.ui.runtime.image.IImage

object ECamelCoreImage {

	val ROUTE_RESOURCE_ICON = ECamelCoreImage2("/icons/route-resource.png")
	val BEAN_ICON = ECamelCoreImage2("/icons/bean.gif")
	val ROUTES_ICON = ECamelCoreImage2("/icons/routes_icon.png")
	val BEAN_WIZ = ECamelCoreImage2("/icons/bean_wiz.png")

	case class ECamelCoreImage2(path: String) extends IImage {

		override def getPath() = path
		override def getLocation() = classOf[ECamelCoreImage2]
	}
}