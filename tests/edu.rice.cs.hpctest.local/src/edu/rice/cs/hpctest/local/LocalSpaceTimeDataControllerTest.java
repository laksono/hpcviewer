package edu.rice.cs.hpctest.local;

import static org.junit.Assert.*;

import org.junit.Test;


public class LocalSpaceTimeDataControllerTest extends BaseLocalTest
{
	@Test
	public void testGetName() {
		for(var controller: list) {
			var name = controller.getName();
			assertNotNull(name);
			assertFalse(name.isEmpty());
		}
	}

	@Test
	public void testSpaceTimeDataControllerLocal() {
		for(var controller: list) {
			var baseData = controller.getBaseData();
			assertNotNull(baseData);
			
			var color = controller.getColorTable();
			assertNotNull(color);
			
			var trace = controller.getCurrentSelectedTraceline();
			assertNull(trace);
		}
	}
}