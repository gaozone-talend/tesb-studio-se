package org.talend.camel.designer.util;

import java.lang.reflect.Method;

import org.junit.Test;

public class CamelFeatureUtilTest {

	@Test
	public void test() throws Exception {
		printGetNameWithoutVersion(null);
		printGetNameWithoutVersion("");
		printGetNameWithoutVersion(".aklsdf");
		printGetNameWithoutVersion("test-213.4.jar");
		printGetNameWithoutVersion("tes123t213.4.jar");
		printGetNameWithoutVersion("-tes1-213.4.jar");
		printGetNameWithoutVersion("22-tes1-213.4.jar");
		printGetNameWithoutVersion("222234.jar");
		printGetNameWithoutVersion("222-234-3.jar");
		printGetNameWithoutVersion("222-234-.jar");
		printGetNameWithoutVersion("-234-.jar");
		printGetNameWithoutVersion("test-234-23-23-24.jar");
		printGetNameWithoutVersion("test-2bbbbbbbbbbb.jar");
		printGetNameWithoutVersion("test-2a-2b-2c.jar");
		printGetNameWithoutVersion("test-a2a-a2b-a2c.jar");
		printGetNameWithoutVersion("test-test-test-test.jar");
		printGetNameWithoutVersion("-test-test-test-tes-t-.jar");
	}

	
	private static void printGetNameWithoutVersion(String libraryName) throws Exception {
		
		Method method = CamelFeatureUtil.class.getDeclaredMethod("getNameWithoutVersion", String.class);
		method.setAccessible(true);
		Object rst = method.invoke(null, libraryName);
		System.out.println(libraryName+"→"+rst);
	}
}
