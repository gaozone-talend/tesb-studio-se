package org.talend.camel.designer.util;

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.talend.camel.core.model.camelProperties.CamelProcessItem;

public class CamelSpringUtil {

	private static String TMP_SPRING_CONTENT = null;

	private CamelSpringUtil() {
	}

	public static String getDefaultContent(CamelProcessItem item) {
		if (TMP_SPRING_CONTENT == null) {
			TMP_SPRING_CONTENT = getTmpSpringContent();
		}

		return TMP_SPRING_CONTENT.replace("${RouteItemName}", item.getProperty().getLabel());
	}

	private static String getTmpSpringContent() {
		InputStream is = null;
		try {
			is = CamelSpringUtil.class.getResourceAsStream("spring.xml");
			return IOUtils.toString(is);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
}
