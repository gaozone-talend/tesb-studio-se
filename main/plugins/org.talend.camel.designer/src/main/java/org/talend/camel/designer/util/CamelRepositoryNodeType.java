// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.camel.designer.util;

import java.util.Map;

import org.talend.core.model.repository.ERepositoryObjectType;

import com.google.common.collect.ImmutableMap;

/**
 * DOC guanglong.du class global comment. Detailled comment
 */
public interface CamelRepositoryNodeType {

	String ROUTES = "ROUTES";

	String BEANS = "BEANS";

	String ROUTE_RESOURCES = "ROUTE_RESOURCES";

	ERepositoryObjectType repositoryRoutesType = getType(ROUTES);

	ERepositoryObjectType repositoryBeansType =getType(BEANS);

	ERepositoryObjectType repositoryRouteResourceType = getType(ROUTE_RESOURCES);

	// repository type and folder name Map
	Map<ERepositoryObjectType, String> AllRouteRespositoryTypes = ImmutableMap.of(
			repositoryBeansType, "Bean",
			repositoryRouteResourceType, "Resource",
			repositoryRoutesType, "Route");

	static ERepositoryObjectType getType(String key) {
		return ERepositoryObjectType.valueOf(
				ERepositoryObjectType.class, key);
	}
}
