package org.talend.camel.designer.util

import org.talend.camel.core.model.camelProperties.CamelProcessItem
import java.util.Scanner

object CamelSpringUtil {

  var TMP_SPRING_CONTENT: Option[String] = None

  def getDefaultContent(item: CamelProcessItem): String = {
    if (TMP_SPRING_CONTENT.isEmpty) {
      TMP_SPRING_CONTENT = Some(getTmpSpringContent())
    }
    TMP_SPRING_CONTENT.get.replace("${RouteItemName}", item.getProperty().getLabel())
  }

  def getTmpSpringContent(): String = {
    try {
      val is = this.getClass().getResourceAsStream("spring.xml")
      val defaultContent = new Scanner(is).useDelimiter("\\A").next()
      is.close
      defaultContent
    } catch {
      case e: Exception =>
        e.printStackTrace()
        ""
    }
  }
}