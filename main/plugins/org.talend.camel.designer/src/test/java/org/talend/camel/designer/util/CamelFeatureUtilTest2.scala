package org.talend.camel.designer.util

import org.junit.Test

class CamelFeatureUtilTest2 {

	@Test def testReadFeatures() {
		var map = CamelFeatureUtil2.readFeaturesFromFile();
		map.foreach((e) => {
			println(e._1)
			e._2.foreach(println)
			println()
		})
	}
}


