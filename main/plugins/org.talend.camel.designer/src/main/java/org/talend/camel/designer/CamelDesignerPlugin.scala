package org.talend.camel.designer

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext
import org.talend.designer.runprocess.IRunProcessService

/**
 * The activator class controls the plug-in life cycle
 */
class CamelDesignerPlugin extends AbstractUIPlugin {
  import CamelDesignerPlugin._

  override def start(context: BundleContext) = {
    super.start(context)
    pluginOpt = Some(this)
  }

  override def stop(context:BundleContext) = {
    pluginOpt = None
    super.stop(context)
  }
}

object CamelDesignerPlugin {
  import org.talend.core.GlobalServiceRegister
  // The plug-in ID
  val PLUGIN_ID = "org.talend.camel.designer"

  // The shared instance
  var pluginOpt: Option[CamelDesignerPlugin] = None

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  def getDefault: CamelDesignerPlugin = { pluginOpt.get }

  /**
   * Returns an image descriptor for the image file at the given plug-in relative path
   *
   * @param path the path
   * @return the image descriptor
   */
  def getImageDescriptor(path: String) =
    AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path)

  def getRunProcessService: IRunProcessService = {
    var service = GlobalServiceRegister.getDefault().getService(classOf[IRunProcessService])
    service.asInstanceOf[IRunProcessService]
  }
}