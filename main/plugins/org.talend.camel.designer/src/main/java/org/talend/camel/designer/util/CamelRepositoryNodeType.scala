package org.talend.camel.designer.util

import org.talend.core.model.repository.ERepositoryObjectType
import java.util.HashMap

object CamelRepositoryNodeType {

	def ROUTES = "ROUTES"
	def BEANS = "BEANS"
	def ROUTE_RESOURCES = "ROUTE_RESOURCES"

	val repositoryRoutesType: ERepositoryObjectType = ERepositoryObjectType.valueOf(
		classOf[ERepositoryObjectType], ROUTES)

	val repositoryBeansType: ERepositoryObjectType = ERepositoryObjectType.valueOf(
		classOf[ERepositoryObjectType], BEANS)

	val repositoryRouteResourceType: ERepositoryObjectType = ERepositoryObjectType.valueOf(
		classOf[ERepositoryObjectType], ROUTE_RESOURCES)

	//repository type and folder name Map
	val AllRouteRespositoryTypes = new HashMap[ERepositoryObjectType, String]() {
		put(repositoryBeansType, "Bean")
		put(repositoryRouteResourceType, "Resource")
		put(repositoryRoutesType, "Route")
	};
}