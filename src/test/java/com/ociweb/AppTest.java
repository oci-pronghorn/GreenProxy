package com.ociweb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.pronghorn.stage.scheduling.NonThreadScheduler;

/**
 * Unit test for simple App.
 */
public class AppTest { 

	
	 @Test
	    public void testApp()
	    {
			long timeoutMS = 10_000;
		    GreenRuntime.testUntilShutdownRequested(new GreenProxy(), timeoutMS);
	    }
}
