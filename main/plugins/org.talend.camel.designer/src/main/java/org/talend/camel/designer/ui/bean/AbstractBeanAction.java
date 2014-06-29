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
package org.talend.camel.designer.ui.bean;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.talend.camel.core.model.camelProperties.BeanItem;
import org.talend.commons.exception.SystemException;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.designer.codegen.ICodeGeneratorService;
import org.talend.designer.codegen.ITalendSynchronizer;
import org.talend.repository.editor.RepositoryEditorInput;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.actions.AContextualAction;

/**
 * DOC Administrator class global comment. Detailled comment
 */
public abstract class AbstractBeanAction extends AContextualAction {

	@Override
	public void init(TreeViewer viewer, IStructuredSelection selection) {
		setEnabled(false);
		Object o = selection.getFirstElement();
		if (selection.isEmpty() || selection.size() != 1 || !(o instanceof RepositoryNode)) {
			return;
		}
		repositoryNode = (RepositoryNode) o;
	}

	public IEditorPart openBeanEditor(BeanItem beanItem, boolean readOnly) throws SystemException, PartInitException {
		if (beanItem == null) {
			return null;
		}
		ICodeGeneratorService service = (ICodeGeneratorService) GlobalServiceRegister.getDefault().getService(
				ICodeGeneratorService.class);

		ECodeLanguage lang = ((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
				.getProject().getLanguage();
		ITalendSynchronizer routineSynchronizer = service.createCamelBeanSynchronizer();

		// check if the related editor is open.
		IWorkbenchPage page = getActivePage();

		String talendEditorID = "org.talend.designer.core.ui.editor.StandAloneTalend" + lang.getCaseName() + "Editor"; //$NON-NLS-1$ //$NON-NLS-2$

		IEditorReference[] editorParts = page.getEditorReferences();
		Optional<IEditorPart> editorOp = Arrays.stream(editorParts)
				.map(r->r.getEditor(false))
				.filter((IEditorPart editor)->{
					if (talendEditorID.equals(editor.getSite().getId())) {
						RepositoryEditorInput editorInput = (RepositoryEditorInput) editor.getEditorInput();
						return editorInput.getItem().equals(beanItem);
					}
					return false;
				}).findFirst();

		editorOp.ifPresent(e->page.bringToTop(e));
		if(editorOp.isPresent()) {
			return editorOp.get();
		}

		routineSynchronizer.syncBean(beanItem, true);
		IFile file = routineSynchronizer.getFile(beanItem);

		RepositoryEditorInput input = new BeanEditorInput(file, beanItem);
		input.setReadOnly(readOnly);
		return page.openEditor(input, talendEditorID);
	}
}
