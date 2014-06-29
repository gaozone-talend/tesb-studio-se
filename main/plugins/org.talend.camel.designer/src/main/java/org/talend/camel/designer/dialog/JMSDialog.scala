package org.talend.camel.designer.dialog

import scala.collection.JavaConversions._
import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.Viewer
import org.eclipse.jface.viewers.LabelProvider
import org.talend.camel.designer.CamelDesignerPlugin
import org.eclipse.swt.graphics.Image
import org.talend.core.model.process.INode
import org.talend.camel.designer.util.ParameterVisitHelper._
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.eclipse.jface.dialogs.Dialog
import org.talend.camel.designer.component.JSMExternalComponentMain
import org.eclipse.swt.widgets.Shell
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.jface.viewers.TreeViewer
import org.talend.camel.designer.dialog.JMSDialog._
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionAdapter
import org.talend.core.model.process.IGEFProcess
import org.talend.core.ui.CoreUIPlugin
import org.eclipse.gef.commands.Command
import org.eclipse.swt.graphics.Point
import org.eclipse.jface.viewers.IStructuredSelection
import org.talend.camel.designer.component.SetConnectionFactoryCommand
import org.eclipse.jface.viewers.StructuredSelection
class JMSDialog private (shell: Shell) extends Dialog(shell) {
		def this(shell: Shell, main: JSMExternalComponentMain) {
			this(shell)
			this.main = main
			initModels()
		}

	var main: JSMExternalComponentMain = _
	private var jmsConnectionFactories: List[INode] = _
	private var treeViewer: TreeViewer = _

	protected override def buttonPressed(buttonId: Int) = {
		buttonId match {
			case IDialogConstants.OK_ID => resetParameter()
			case _ => super.buttonPressed(buttonId)
		}
	}

	protected override def configureShell(newShell: Shell) = {
		super.configureShell(newShell)
		newShell.setText("Select JMS ConnectionFactory:")
	}

	protected override def createDialogArea(parent: Composite) = {
		val container = new Composite(parent, SWT.NONE)
		container.setLayoutData(new GridData(GridData.FILL_BOTH))
		container.setLayout(new GridLayout())

		treeViewer = new TreeViewer(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL)
		val tree = treeViewer.getTree()

		treeViewer.setContentProvider(new ConnectionFactoryContentProvider());
		treeViewer.setLabelProvider(new ConnectionFactoryLabelProvider());
		treeViewer.setInput(jmsConnectionFactories);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		def selectionListenre = new SelectionAdapter() {
			override def widgetDefaultSelected(e: SelectionEvent) = buttonPressed(IDialogConstants.OK_ID)
		}
		tree.addSelectionListener(selectionListenre)
		container
	}

	def executeCommand(cmd: Command) = {
		val process = main.getExternalComponent().getProcess()
		var executed = false
		if (process != null && process.isInstanceOf[IGEFProcess]) {
			val service = CoreUIPlugin.getDefault().getDesignerCoreUIService()
			if (service != null) {
				executed = service.executeCommand(process.asInstanceOf[IGEFProcess], cmd)
			}
		}
		if (!executed) cmd.execute()
	}

	protected override def getInitialSize = new Point(320, 450)

	private def getSelectedNode(): INode = {
		val sselection = treeViewer.getSelection().asInstanceOf[IStructuredSelection]
		sselection.getFirstElement().asInstanceOf[INode]
	}

	private def initModels() = {
		val component = main.getExternalComponent()
		val nodes = component.getProcess().getGraphicalNodes().toList
		jmsConnectionFactories = nodes.filter(n => n.getComponent().getName() == "cJMSConnectionFactory")
	}

	private def resetParameter() = {
		val command = new SetConnectionFactoryCommand(main, getSelectedNode())
		executeCommand(command)
	}

	private def setSelection() = {
		val jmsExternalComponent = main.getExternalComponent();
		val label = jmsExternalComponent \\~ "CONNECTION_FACOTRY_LABEL"
		if (label != null) {
			val value = label.toString()
			for (n <- jmsConnectionFactories) {
				if (value == getLabel(n)) {
					treeViewer.setSelection(new StructuredSelection(n))
				}
			}
		}
	}

}

object JMSDialog {
	class ConnectionFactoryContentProvider extends ITreeContentProvider {
		override def dispose() = {}
		override def inputChanged(viewer: Viewer, oldInput: Object, newInput: Object) = {}

		override def getParent(element: Any) = null

		override def hasChildren(element: Any) = getChildren(element).length > 0
		override def getChildren(parent: Any) = getElements(parent)
		override def getElements(input: Any): Array[Object] = {
			if (input.isInstanceOf[java.util.List[_]]) {
				input.asInstanceOf[java.util.List[Any]].toArray()
			} else {
				new Array[Object](0)
			}
		}
	}

	class ConnectionFactoryLabelProvider extends LabelProvider {
		val image = AbstractUIPlugin.imageDescriptorFromPlugin(CamelDesignerPlugin.PLUGIN_ID,
			"icons/cJMSConnectionFactory_16.png").createImage()

		override def getImage(element: Any): Image = image

		override def getText(element: Any): String = {
			if (element.isInstanceOf[INode]) getLabel(element.asInstanceOf[INode])
			else super.getText(element)
		}

	}

	def getLabel(element: INode): String = {
		val param = element \\~ "LABEL"
		if ("__UNIQUE_NAME__" != param) param.asInstanceOf[String]
		else element.getUniqueName()
	}

}