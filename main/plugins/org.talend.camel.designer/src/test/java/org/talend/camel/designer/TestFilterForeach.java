package org.talend.camel.designer;

import java.util.stream.IntStream;

import org.junit.Test;

public class TestFilterForeach {

	@Test
	public void test() {
		boolean anyMatch = IntStream.range(1, 1000)
		.filter(i->{
			System.out.println("filter :"+i);
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return i<10;
		}).anyMatch(i->{
			System.out.println("anymatch:"+i);
			return i>5;
		});
		System.out.println(anyMatch);
	}

}
