package org.talend.camel.designer.util

import org.junit.Test

class CamelFeatureUtilTest2 {

	@Test def testReadFeatures() {
		var map = CamelFeatureUtil.readFeaturesFromFile()
		map.foreach((e) => {
			println(e._1)
			e._2.foreach(println)
			println()
		})
	}

	@Test def printGetNameWithoutVersions() {
		printGetNameWithoutVersion(null)
		printGetNameWithoutVersion("")
		printGetNameWithoutVersion(".aklsdf")
		printGetNameWithoutVersion("test-213.4.jar")
		printGetNameWithoutVersion("tes123t213.4.jar")
		printGetNameWithoutVersion("-tes1-213.4.jar")
		printGetNameWithoutVersion("22-tes1-213.4.jar")
		printGetNameWithoutVersion("222234.jar")
		printGetNameWithoutVersion("222-234-3.jar")
		printGetNameWithoutVersion("222-234-.jar")
		printGetNameWithoutVersion("-234-.jar")
		printGetNameWithoutVersion("test-234-23-23-24.jar")
		printGetNameWithoutVersion("test-2bbbbbbbbbbb.jar")
		printGetNameWithoutVersion("test-2a-2b-2c.jar")
		printGetNameWithoutVersion("test-a2a-a2b-a2c.jar")
		printGetNameWithoutVersion("test-test-test-test.jar")
		printGetNameWithoutVersion("-test-test-test-tes-t-.jar")

	}

	def printGetNameWithoutVersion(libraryName: String) {
		val rst2 = CamelFeatureUtil.getNameWithoutVersion(libraryName)
		println(libraryName + "→" + rst2)
	}
}


