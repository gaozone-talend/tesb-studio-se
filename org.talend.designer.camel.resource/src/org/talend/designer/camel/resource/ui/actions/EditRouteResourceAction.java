// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.camel.resource.ui.actions;

import java.util.Properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.talend.camel.core.model.camelProperties.RouteResourceItem;
import org.talend.camel.designer.util.CamelRepositoryNodeType;
import org.talend.camel.designer.util.ECamelCoreImage;
import org.talend.commons.ui.runtime.exception.MessageBoxExceptionHandler;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.core.model.properties.Property;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.designer.camel.resource.editors.ResourceEditorListener;
import org.talend.designer.camel.resource.editors.RouteResourceEditor;
import org.talend.designer.camel.resource.editors.input.RouteResourceInput;
import org.talend.designer.camel.resource.i18n.Messages;
import org.talend.designer.core.DesignerPlugin;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.BinRepositoryNode;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.ui.actions.AContextualAction;
import org.talend.repository.ui.views.IRepositoryView;

/**
 * DOC smallet class global comment. Detailled comment <br/>
 * 
 * $Id: EditProcess.java 52559 2010-12-13 04:14:06Z nrousseau $
 * 
 */
public class EditRouteResourceAction extends AContextualAction {

	private Properties params;

	public EditRouteResourceAction() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	protected void doRun() {
		ISelection selection = getSelectedObject();
		if (selection == null) {
			return;
		}
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj == null) {
			return;
		}
		RepositoryNode node = (RepositoryNode) obj;
		openOrBindEditor(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.talend.repository.ui.actions.AContextualView#getClassForDoubleClick()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class getClassForDoubleClick() {
		return RouteResourceItem.class;
	}

	private ISelection getSelectedObject() {
		if (params == null) {
			return getSelection();
		} else {
			RepositoryNode repositoryNode = RepositoryNodeUtilities
					.getRepositoryNode(params.getProperty("nodeId"), false); //$NON-NLS-1$
			IRepositoryView viewPart = getViewPart();
			if (repositoryNode != null && viewPart != null) {
				RepositoryNodeUtilities.expandParentNode(viewPart,
						repositoryNode);
				return new StructuredSelection(repositoryNode);
			}
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.talend.repository.ui.actions.ITreeContextualAction#init(org.eclipse
	 * .jface.viewers.TreeViewer,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(TreeViewer viewer, IStructuredSelection selection) {
		boolean canWork = !selection.isEmpty() && selection.size() == 1;
		IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
		if (factory.isUserReadOnlyOnCurrentProject()) {
			canWork = false;
		}
		if (canWork) {
			Object o = selection.getFirstElement();
			RepositoryNode node = (RepositoryNode) o;
			switch (node.getType()) {
			case REPOSITORY_ELEMENT:
				if (node.getObjectType() != CamelRepositoryNodeType.repositoryRouteResourceType) {
					canWork = false;
				} else {
					IRepositoryService service = DesignerPlugin.getDefault()
							.getRepositoryService();
					IProxyRepositoryFactory repFactory = service
							.getProxyRepositoryFactory();
					if (repFactory.isPotentiallyEditable(node.getObject())) {
						this.setText(""); //$NON-NLS-1$
					} else {
						this.setText(""); //$NON-NLS-1$
					}
				}
				break;
			default:
				canWork = false;
			}
			RepositoryNode parent = node.getParent();
			if (canWork && parent != null
					&& parent instanceof BinRepositoryNode) {
				canWork = false;
			}
			if (canWork
					&& !ProjectManager.getInstance().isInCurrentMainProject(
							node)) {
				canWork = false;
			}

			// If the editProcess action canwork is true, then detect that the
			// job version is the latest verison or not.
			if (canWork) {
				canWork = isLastVersion(node);
			}

		}
		setEnabled(canWork);

		this.setText(Messages.getString("EditRouteResourceAction_Title"));
		this.setToolTipText(Messages
				.getString("EditRouteResourceAction_Tooltip"));
		this.setImageDescriptor(ImageProvider
				.getImageDesc(ECamelCoreImage.ROUTE_RESOURCE_ICON));
	}

	/**
	 * Open or bind RouteResourceEditor
	 * 
	 * @param node
	 */
	private void openOrBindEditor(RepositoryNode node) {

		Property property = (Property) node.getObject().getProperty();
		RouteResourceItem item = null;
		if (property != null) {

			Assert.isTrue(property.getItem() instanceof RouteResourceItem);
			item = (RouteResourceItem) property.getItem();
			IWorkbenchPage page = getActivePage();
			openEditor(page, node, item);
		}

	}

	/**
	 * Open or bind Route resource editor.
	 * 
	 * @param page
	 * @param node
	 * @param item
	 */
	public static void openEditor(final IWorkbenchPage page,
			IRepositoryNode node,
			RouteResourceItem item) {
		try {

			RouteResourceInput fileEditorInput = RouteResourceInput
					.createInput(item);

			fileEditorInput.setRepositoryNode(node);

			IEditorRegistry editorRegistry = PlatformUI.getWorkbench()
					.getEditorRegistry();
			String extension = item.getFileExtension();
			IEditorDescriptor defaultEditor = editorRegistry
					.getDefaultEditor("*." + extension);
			String editorId = null;
			if (defaultEditor != null) {
				editorId = defaultEditor.getId();
			} else {
				editorId = RouteResourceEditor.ID;
			}

			IEditorPart editorPart = page.findEditor(fileEditorInput);

			if (editorPart == null) {
				editorPart = page.openEditor(fileEditorInput, editorId, true);

			} else {
				editorPart = page.openEditor(fileEditorInput, editorId);
			}
			page.getWorkbenchWindow()
					.getPartService()
					.addPartListener(
							new ResourceEditorListener(fileEditorInput, page));

		} catch (Exception e) {
			MessageBoxExceptionHandler.process(e);
		}
	}
}
