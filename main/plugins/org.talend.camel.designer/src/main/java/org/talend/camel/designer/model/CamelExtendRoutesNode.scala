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
package org.talend.camel.designer.model;

import org.talend.core.repository.IExtendRepositoryNode
import org.talend.camel.designer.util.ECamelCoreImage
import org.talend.commons.ui.runtime.image.IImage

class CamelExtendRoutesNode extends IExtendRepositoryNode {
	
	override def getNodeImage(): IImage = ECamelCoreImage.ROUTES_ICON

	override def getOrdinal() = 5

	override def getChildren(): Array[Object] = Array.empty

}
