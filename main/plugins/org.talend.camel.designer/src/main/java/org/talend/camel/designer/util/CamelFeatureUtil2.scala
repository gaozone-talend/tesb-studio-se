package org.talend.camel.designer.util

import scala.collection.JavaConversions._
import scala.xml._
import scala.collection._
import scala.collection.generic._
import org.talend.camel.designer.util.ParameterVisitHelper._
import org.talend.designer.publish.core.models.FeaturesModel
import org.talend.designer.publish.core.models.FeatureModel
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
import scala.annotation.tailrec
import scala.collection.immutable.Nil
import scala.io.Source
import scala.collection.mutable.MutableList
import org.talend.designer.core.model.utils.emf.talendfile.ElementValueType
import org.talend.designer.core.model.utils.emf.talendfile.ConnectionType
import org.talend.designer.core.model.components.EParameterName

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

	private var camelFeaturesMap: Map[String, Seq[FeatureModel]] = _

	private val JAVA_SCRIPT = "javaScript"

	private val LANGUAGES = "LANGUAGES"
	private val LOOP_TYPE = "LOOP_TYPE"

	/**
	 * Check the node is Route.
	 *
	 * @param nodeOp the node op
	 */
	private def checkNode(nodeOp: Option[IRepositoryNode]): Boolean = {
		nodeOp.map(_.getObjectType() == CamelRepositoryNodeType.repositoryRoutesType)
			.getOrElse(false)
	}

	private def computeFeature(libraryName: String): Seq[FeatureModel] = {
		if (camelFeaturesMap.isEmpty) initMap()
		val nameWithoutVersion = getNameWithoutVersion(libraryName)
		camelFeaturesMap.get(nameWithoutVersion).get
	}

	def getNameWithoutVersion(libraryName: String): String = {
		if (libraryName == null || libraryName.isEmpty() || !libraryName.endsWith(".jar")) {
			libraryName
		}

		@tailrec
		def removeVersionPart(lib: String): String = {
			val lastIndexOf = lib.lastIndexOf('-')
			if (lastIndexOf == -1) lib
			val followChar = lib.charAt(lastIndexOf + 1)
			val isFollowNum = followChar >= '0' && followChar <= '9'
			if (isFollowNum) lib
			removeVersionPart(lib.substring(0, lastIndexOf))
		}
		removeVersionPart(libraryName)
	}

	private def getFeaturesOfRoute(neededLibraries: Iterable[String], processType: ProcessType): Iterable[FeatureModel] = {
		val features = Iterable[FeatureModel]()
		for (lib <- neededLibraries) {
			val featureModel = computeFeature(lib)
			featureModel.foreach(features.add(_))
		}
		addProcessSpecialFeatures(features, processType)
		features
	}

	private def addProcessSpecialFeatures(features: Iterable[FeatureModel], processType: ProcessType) = {
		addNodesSpecialFeatures(_, _)
		addConnectionsSpecialFeatures(_, _)
	}

	private def handleJmsConnectionFactory(features: Iterable[FeatureModel], currentNode: NodeType) = {
		val isActiveMQ = currentNode \\ "MQ_TYPE" \== "ActiveMQ"
		if (!isActiveMQ) None
		val useHttpBroker = currentNode \\ "IS_AMQ_HTTP_BROKER" \ Boolean
		if (useHttpBroker) features.add(FEATURE_ACTIVEMQ_OPTIONAL)
	}

	private def addConnectionsSpecialFeatures(features: Iterable[FeatureModel], processType: ProcessType) = {
		var conns = processType.getConnection().toTraversable
			.filter(_.isInstanceOf[ConnectionType])
			.map(_.asInstanceOf[ConnectionType])
		for (conn <- conns) {
			val routeType = conn \\~ EParameterName.ROUTETYPE.getName()
			routeType match {
				case "groovy" => features.add(FEATURE_CAMEL_GROOVY)
				case "javaScript" => {
					features.add(FEATURE_CAMEL_SCRIPT)
					features.add(FEATURE_CAMEL_SCRIPT_JAVASCRIPT)
				}
				case _ =>
			}
		}
	}

	private def initMap() = {
		camelFeaturesMap = readFeaturesFromFile()
	}

	private def addNodesSpecialFeatures(features: Iterable[FeatureModel], processType: ProcessType) = {
		var nodes = processType.getNode().toTraversable
			.filter(_.isInstanceOf[NodeType])
			.map(_.asInstanceOf[NodeType])
		for (currentNode <- nodes) {
			val componentName = currentNode.getComponentName()
			componentName match {
				case "cCXF" | "cCXFRS" => handleCXFcase(features, currentNode)
				case "cLoop" => handleCXFcase(features, currentNode)
				case "cMessageFilter" => handleMessageFilterCase(features, currentNode)
				case "cRecipientList" => handleRecipientListCase(features, currentNode)
				case "cSetBody" => handleSetBodyCase(features, currentNode)
				case "cSetHeader" => handleSetHeaderCase(features, currentNode)
				case "cJMSConnectionFactory" => handleJmsConnectionFactory(features, currentNode)
				case _ => None
			}
		}
	}

	protected def handleSetHeaderCase(features: Iterable[FeatureModel], currentNode: NodeType) = {
		val values = currentNode \\ "VALUES"
		val exists = (values.getElementValue().toTraversable).exists(v => {
			if (!v.isInstanceOf[ElementValueType]) false
			val evt = v.asInstanceOf[ElementValueType]
			"LANGUAGE" == evt.getElementRef() && JAVA_SCRIPT == evt.getValue()
		})
		if (exists) features.add(FEATURE_CAMEL_SCRIPT_JAVASCRIPT)
	}

	protected def handleSetBodyCase = handleMessageFilterCase(_, _)

	protected def handleRecipientListCase = handleMessageFilterCase(_, _)

	protected def handleMessageFilterCase(features: Iterable[FeatureModel], currentNode: NodeType) = {
		val isJS = currentNode \\ LANGUAGES \== JAVA_SCRIPT
		if (isJS) features.add(FEATURE_CAMEL_SCRIPT_JAVASCRIPT)
	}

	protected def handleLoopCase(features: Iterable[FeatureModel], currentNode: NodeType) = {
		val isExpType = currentNode \\ LOOP_TYPE \== "EXPRESSION_TYPE"
		if (isExpType) handleMessageFilterCase(_, _)

	}

	protected def handleCXFcase(features: Iterable[FeatureModel], currentNode: NodeType) = {
		val sam = currentNode \\ "ENABLE_SAM" \ Boolean
		if (sam) features.add(FEATURE_ESB_SAM)
		val sl = currentNode \\ "ENABLE_SL" \ Boolean
		// http://jira.talendforge.org/browse/TESB-5461
		if (sl) features.add(FEATURE_ESB_LOCATOR)
	}

	/**
	 * Add feature and bundle to Feature Model
	 */
	def addFeatureAndBundles(node: IRepositoryNode, featuresModel: FeaturesModel) = {
		if (!checkNode(Option(node))) None
		val property = node.getObject().getProperty()
		val process = new RouteProcess(property)
		process.loadXmlFile()

		val neededLibraries = process.getNeededLibraries(true)
		val processType = property.getItem().asInstanceOf[ProcessItem]
		val features = getFeaturesOfRoute(neededLibraries, (processType.getProcess()))
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

	def readFeaturesFromFile(): Map[String, Seq[FeatureModel]] = {
		val input = this.getClass().getResourceAsStream(MAPPING_XML_FILE)
		val xml = scala.xml.XML.load(input)
		val map = for (featureMap <- xml \\ "FeatureMap") yield {
			val hotLib = featureMap \@ "HotLib"
			val models = for (feature <- featureMap \ "Feature") yield {
				val version = feature \@ "version"
				val name = feature.text
				new FeatureModel(name, version)
			}
			(hotLib -> models)
		}
		//after groupBy, key is Seq[(String, Seq[])] 
		map.groupBy(_._1).mapValues(seq => seq.map(t => t._2).flatten)
	}
}