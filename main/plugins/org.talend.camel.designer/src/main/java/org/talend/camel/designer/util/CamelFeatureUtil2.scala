package org.talend.camel.designer.util

import scala.collection.JavaConversions._
import scala.collection._
import scala.collection.generic._
import org.talend.designer.publish.core.models.FeaturesModel
import org.talend.designer.publish.core.models.FeatureModel
import scala.collection.mutable.Map
import org.apache.commons.lang.mutable.Mutable
import org.talend.repository.model.IRepositoryNode
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType
import org.talend.designer.core.model.components.ElementParameter
import org.talend.core.model.utils.JavaResourcesHelper
import org.talend.core.model.properties.Item
import org.talend.camel.designer.ui.editor.RouteProcess
import org.talend.core.model.properties.ProcessItem
import scala.collection.Traversable
import org.talend.designer.core.model.utils.emf.talendfile.NodeType
import scala.collection.mutable.ArrayBuffer
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType
import org.talend.designer.core.model.utils.emf.talendfile.impl.NodeTypeImpl
import orgomg.cwm.objectmodel.core.Feature
import scala.collection.mutable.Iterable

/**
 *  http://jira.talendforge.org/browse/TESB-5375
 */
object CamelFeatureUtil2 {

	private val MAPPING_XML_FILE = "CamelFeatures.xml"
	private val CAMEL_VERSION_RANGE = "[2,5)"

	private val FEATURE_CAMEL_GROOVY = new FeatureModel("camel-groovy", CAMEL_VERSION_RANGE)
	private val FEATURE_CAMEL_SCRIPT_JAVASCRIPT = new FeatureModel("camel-script-javascript", CAMEL_VERSION_RANGE)
	private val FEATURE_CAMEL_SCRIPT = new FeatureModel("camel-script", CAMEL_VERSION_RANGE)

	private val FEATURE_ACTIVEMQ_OPTIONAL = new FeatureModel("activemq-http", "[5,10)")
	private val FEATURE_ESB_SAM = new FeatureModel("tesb-sam-agent", FeaturesModel.ESB_FEATURE_VERSION_RANGE)
	private val FEATURE_ESB_LOCATOR = new FeatureModel("tesb-locator-client", FeaturesModel.ESB_FEATURE_VERSION_RANGE)

	private val camelFeaturesMap = Map[String, Iterable[FeatureModel]]()

	private val JAVA_SCRIPT = "javaScript"

	private val LANGUAGES = "LANGUAGES"
	private val LOOP_TYPE = "LOOP_TYPE"

	/**
	 * Check the node is Route.
	 *
	 * @param nodeOp the node op
	 */
	private def checkNode(nodeOp: Option[IRepositoryNode]): Boolean = nodeOp.map(
		_.getObjectType() == CamelRepositoryNodeType.repositoryRoutesType).getOrElse(false)

	/**
	 * Compute check field parameter value with a given parameter name
	 *
	 */
	protected def computeCheckElementValue(paramName: String, elementPrameterTypes: Traversable[Any]): Boolean = {
		val cpTypeOp = findElementParameterByName(paramName, elementPrameterTypes)
		cpTypeOp.map(_.getValue() == "true").getOrElse(false)
	}

	protected def findElementParameterByName(
		paramName: String, elementParameterTypes: Traversable[Any]): Option[ElementParameterType] = {
		val op = elementParameterTypes.find(_.asInstanceOf[ElementParameterType].getName() == paramName)
		op.map(_.asInstanceOf[ElementParameterType])
	}
	
	private def getFeaturesOfRoute(neededLibraries:Iterable[String], processType:ProcessType):Iterable[FeatureModel] = {
		val features = Iterable[FeatureModel]()
		for(lib <- neededLibraries) {
			val featureModel = computFeature(lib)
			featureModel.foreach(features.add(_))
		}
		addProcessSpecialFeatures(features, processType)
		features
	}
	
	private def computeFeature(libraryName:String) = {
		if(camelFeaturesMap .isEmpty) initMap()
		val nameWithoutVersion = getNameWithoutVersion(libraryName)
		camelFeaturesMap.get(nameWithoutVersion)
	}
	
	private def getNameWithoutVersion(libraryName:String):String = {
		if(libraryName == null || libraryName.isEmpty() || !libraryName.endsWith(".jar")){
			libraryName
		}
		var interName = libraryName
		var lastIndexOf = interName.lastIndexOf('-')
		while(lastIndexOf != -1) {
			try{
				Integer.parseInt(Character.toString(interName.charAt(lastIndexOf+1)));
				interName = interName.substring(0, lastIndexOf);
				break;
			}catch(Exception e){
				interName = interName.substring(0, lastIndexOf);
				lastIndexOf = interName.lastIndexOf('-');
			}
		}
		interName
	}
	
	private def addNodesSpecialFeatures(features:Iterable[FeatureModel], processType:ProcessType) = {
		var nodes = processType.getNode().toTraversable
			.filter(_.isInstanceOf[NodeType])
			.map(_.asInstanceOf[NodeType])
		for(currentNode <- nodes) {
			val componentName = currentNode.getComponentName()
			componentName match {
			  case "cCXF"|"cCXFRS" => handleCXFcase(features, currentNode)
			  case "cLoop" => handleCXFcase(features, currentNode)
			  case "cMessageFilter" => handleCXFcase(features, currentNode)
			  case "cRecipientList" => handleCXFcase(features, currentNode)
			  case "cSetBody" => handleCXFcase(features, currentNode)
			  case "cSetHeader" => handleCXFcase(features, currentNode)
			  case "cJMSConnectionFactory" => handleCXFcase(features, currentNode)
			  case _=>None
			}
		}
	}

	protected def handleLoopCase(features:ArrayBuffer[FeaturesModel], currentNode:NodeType) = {
		
	}
	
	protected def handleCXFcase(features:Iterable[FeatureModel], currentNode:NodeType) = {
		var params = currentNode.getElementParameter().toTraversable
		var sam = computeCheckElementValue("ENABLE_SAM", params)
		if(sam) features.add(FEATURE_ESB_SAM)
		var sl = computeCheckElementValue("ENABLE_SL", currentNode.getElementParameter())
		if(sl) features.add(FEATURE_ESB_LOCATOR) 
	}
	
	/**
	 * Add feature and bundle to Feature Model
	 */
	def addFeatureAndBundles(node:IRepositoryNode, featuresModel:FeaturesModel) = {
		if(!checkNode(Option(node))) None
		val property = node.getObject().getProperty()
		val process = new RouteProcess(property)
		process.loadXmlFile()
		
		val neededLibraries = process.getNeededLibraries(true)
		val features = getFeaturesOfRoute(neededLibraries, ((ProcessItem) property.getItem()).getProcess())
		features.foreach(featuresModel.addFeature(_))
		process.dispose()
	}
	
	def getMavenGroupId(item: Item): String = {
		if (item != null) {
			val projectName = JavaResourcesHelper.getProjectFolderName(item)
			val itemName = item.getProperty().getDisplayName()
			s"$projectName.$itemName"
		}
		null
	}
}