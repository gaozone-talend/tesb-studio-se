package org.talend.camel.designer.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

public class CamelSpringUtilTest {

	@Test
	public void getTmpSpringContentTest() throws Exception {
		String rst = Whitebox.invokeMethod(CamelSpringUtil.class, "getTmpSpringContent");
		assertNotNull(rst);
		assertTrue(rst.length() > 500);
		assertThat(rst, containsString("${RouteItemName}"));
	}

}
