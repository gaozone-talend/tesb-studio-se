package org.talend.camel.designer.util
import scala.collection.JavaConversions._
import java.util.List
import org.talend.designer.core.model.utils.emf.talendfile.NodeType
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType
import org.talend.designer.core.model.utils.emf.talendfile.ConnectionType
object ParameterVisitHelper {

	implicit class NodeTypeHelper(n: NodeType) {
		def \\(paramName: String): ElementParameterType = {
			val params = n.getElementParameter.toTraversable
			params.map(_.asInstanceOf[ElementParameterType])
				.find(p => p.getName() == paramName).get
		}

		def \\~(paramName: String): String = {
			(n \\ (paramName)).getValue()
		}

	}

	implicit class ConnectionTypeHelper(n: ConnectionType) {
		def \\(paramName: String): ElementParameterType = {
			val params = n.getElementParameter.toTraversable
			params.map(_.asInstanceOf[ElementParameterType])
				.find(p => p.getName() == paramName).get
		}

		def \\~(paramName: String): String = {
			(n \\ (paramName)).getValue()
		}

	}

	//ConnectionType.getElementParameter()

	implicit class ElementParameterTypeHelper(t: ElementParameterType) {

		/**
		 * $bslash. getBoolean value
		 *
		 * @param getType the get type
		 * @return true, if successful
		 */
		def \(getType: Boolean.type): Boolean = {
			t.getValue() == "true"
		}

		def \(): String = {
			t.getValue()
		}

		/**
		 * $bslash$eq$eq. if value equals
		 *
		 * @param value the value
		 * @return true, if successful
		 */
		def \==(value: String): Boolean = {
			t.getValue() == value
		}

		def \!=(value: String): Boolean = {
			t.getValue() != value
		}
	}
}