package org.talend.designer.camel.dependencies.ui.dialog;

import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.talend.designer.camel.dependencies.core.model.IDependencyItem;
import org.talend.designer.camel.dependencies.core.model.ImportPackage;
import org.talend.designer.camel.dependencies.core.model.OsgiDependencies;
import org.talend.designer.camel.dependencies.core.model.RequireBundle;
import org.talend.designer.camel.dependencies.ui.Messages;

public class NewOrEditDependencyDialog extends TitleAreaDialog {

	private int type;
	private OsgiDependencies<?> item;
	private List<?> input;

	private boolean isNew = false;
	
	private boolean isChanged = false;

	public NewOrEditDependencyDialog(List<?> input, Shell parentShell, int type) {
		super(parentShell);
		this.input = input;
		this.type = type;
		this.isNew = true;
		switch (type) {
		case IDependencyItem.IMPORT_PACKAGE:
			item = new ImportPackage();
			break;
		case IDependencyItem.REQUIRE_BUNDLE:
			item = new RequireBundle();
			break;
		default:
			break;
		}
	}

	public NewOrEditDependencyDialog(OsgiDependencies<?> sourceItem, Shell parentShell,
			int type) {
		super(parentShell);
		this.item = sourceItem;
		this.type = type;
		this.isNew = false;
		switch (type) {
		case IDependencyItem.IMPORT_PACKAGE:
			this.item = new ImportPackage((ImportPackage)sourceItem);
			break;
		case IDependencyItem.REQUIRE_BUNDLE:
			this.item = new RequireBundle((RequireBundle)sourceItem);
			break;
		default:
			break;
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		if(isNew){
			switch (type) {
			case IDependencyItem.IMPORT_PACKAGE:
				getShell().setText(Messages.NewDependencyItemDialog_addImportPackage);
				setTitle(Messages.NewDependencyItemDialog_importPackage);
				setMessage(Messages.NewDependencyItemDialog_importPackageMessage);
				break;
			case IDependencyItem.REQUIRE_BUNDLE:
				getShell().setText(Messages.NewDependencyItemDialog_addRequireBundle);
				setTitle(Messages.NewDependencyItemDialog_requireBundle);
				setMessage(Messages.NewDependencyItemDialog_addRequireBundleMsg);
				break;

			default:
				break;
			}
		} else {
			switch (type) {
			case IDependencyItem.IMPORT_PACKAGE:
				getShell().setText(Messages.NewDependencyItemDialog_editImportPackageTitle);
				setTitle(Messages.NewDependencyItemDialog_importPackage);
				setMessage(Messages.NewDependencyItemDialog_editImportPackageMsg);
				break;
			case IDependencyItem.REQUIRE_BUNDLE:
				getShell().setText(Messages.NewDependencyItemDialog_editRequireBundleTitle);
				setTitle(Messages.NewDependencyItemDialog_requireBundle);
				setMessage(Messages.NewDependencyItemDialog_editRequireBundleMsg);
				break;

			default:
				break;
			}
		}

		parent.setLayout(new GridLayout(1, false));

		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

		new Label(c, SWT.NONE).setText(Messages.NewDependencyItemDialog_name);

		Text nameT = new Text(c, SWT.BORDER);
		nameT.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		new Label(c, SWT.NONE)
				.setText(Messages.NewDependencyItemDialog_minVersion);
		Text minVersionT = new Text(c, SWT.BORDER);
		minVersionT.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		new Label(c, SWT.NONE)
				.setText(Messages.NewDependencyItemDialog_maxVersion);
		Text maxVersionT = new Text(c, SWT.BORDER);
		maxVersionT.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		new Label(c, SWT.NONE);

		Button optionalBtn = new Button(c, SWT.CHECK);
		optionalBtn.setText(Messages.NewDependencyItemDialog_optional);

		if(!isNew){
			nameT.setText(item.getName()==null?"":item.getName()); //$NON-NLS-1$
			minVersionT.setText(item.getMinVersion()==null?"":item.getMinVersion()); //$NON-NLS-1$
			maxVersionT.setText(item.getMaxVersion()==null?"":item.getMaxVersion()); //$NON-NLS-1$
			optionalBtn.setSelection(item.isOptional());
		}
		nameT.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				item.setName(((Text) e.getSource()).getText());
				isChanged = true;
				getButton(OK).setEnabled(validate());
			}
		});
		minVersionT.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				item.setMinVersion(((Text) e.getSource()).getText());
				isChanged = true;
				getButton(OK).setEnabled(validate());
			}
		});
		maxVersionT.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				item.setMaxVersion(((Text) e.getSource()).getText());
				isChanged = true;
				getButton(OK).setEnabled(validate());
			}
		});
		optionalBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				isChanged = true;
				item.setOptional(((Button) e.getSource()).getSelection());
				getButton(OK).setEnabled(validate());
			}
		});
		return c;
	}


	private boolean validate() {
		if (isNew && input.contains(item)) {
			setErrorMessage(Messages.NewDependencyItemDialog_existCheckMessage);
			return false;
		}
		int valid = item.isValid();
		switch (valid) {
		case OsgiDependencies.OK:
			break;
		case OsgiDependencies.NAME_NULL:
			setErrorMessage(Messages.NewDependencyItemDialog_nameIsNullMessage);
			return false;
		case OsgiDependencies.NAME_INVALID:
			setErrorMessage(Messages.NewOrEditDependencyDialog_nameInvalidMsg);
			return false;
		case OsgiDependencies.MIN_INVALID:
			setErrorMessage(Messages.NewDependencyItemDialog_minVersionInvalidMsg);
			return false;
		case OsgiDependencies.MAX_INVALID:
			setErrorMessage(Messages.NewDependencyItemDialog_maxVersionInvalidMsg);
			return false;
		case OsgiDependencies.MIN_MAX_INVALID:
			setErrorMessage(Messages.NewDependencyItemDialog_minVersionGreatThanMaxVersionMsg);
			return false;
		default:
			break;
		}
		setErrorMessage(null);
		return true;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
	}

	public OsgiDependencies<?> getDependencyItem() {
		return item;
	}
	
	public boolean isChanged() {
		return isChanged;
	}
}