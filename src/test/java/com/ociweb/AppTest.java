package com.ociweb;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;

/**
 * Unit test for simple App.
 */
public class AppTest { 

		private final long timeoutMS = 9_000;
	    
		@Test
	    public void testBackingServerSequential() {
	    	GreenRuntime.run(new TestServer(8082));
	    	waitForServer("http://127.0.0.1:8082/");
		    GreenRuntime.testUntilShutdownRequested(new TestClientSequential(12000,8082), timeoutMS);	    	
	    }
	    
	    @Test
	    public void testBackingServerConcurrent() {
	    	GreenRuntime.run(new TestServer(8082));
	    	waitForServer("http://127.0.0.1:8082/");
	    	GreenRuntime.testConcurrentUntilShutdownRequested(new TestClientParallel(12000,8082), timeoutMS);
        }
		
		
		@Test
	    public void textProxyServer() {
			
			//startup backing server
			GreenRuntime.run(new TestServer(8082));
	    	waitForServer("http://127.0.0.1:8082/");
	    	
	    	//startup proxy server in front of backing server
	    	GreenRuntime.run(new GreenProxy("127.0.0.1",8082)); // 8786
	    	waitForServer("http://127.0.0.1:8786/");
	    	
	    	//GreenRuntime.testConcurrentUntilShutdownRequested(new TestClientParallel(12000,8786), timeoutMS);	 
		    GreenRuntime.testUntilShutdownRequested(new TestClientSequential(12000,8786), timeoutMS);	
	    }

		private void waitForServer(String url) {
			try {
			
				waitForServer(new URL(url));
			
			} catch (MalformedURLException e) {
				
				e.printStackTrace();
				fail();
			}
		}
		
		private void waitForServer(URL url) {
			try {
				boolean waiting = true;				
				while (waiting) {
					try {
						URLConnection con = url.openConnection();				
						con.connect();
					} catch (ConnectException ce) {
						continue;
					}
					waiting = false;
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		
}
