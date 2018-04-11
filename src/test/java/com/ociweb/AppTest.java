package com.ociweb;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Ignore;
import org.junit.Test;

import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.test.ParallelClientLoadTester;
import com.ociweb.gl.test.ParallelClientLoadTesterConfig;
import com.ociweb.gl.test.ParallelClientLoadTesterPayload;
import com.ociweb.pronghorn.network.http.HTTP1xResponseParserStage;
import com.ociweb.pronghorn.network.http.HTTP1xRouterStage;
import com.ociweb.pronghorn.network.http.HTTPClientRequestStage;

/**
 * Unit test for simple App.
 */
public class AppTest { 

		private final long timeoutMS = 260_000;
		private final byte[] data = "exampleResponse".getBytes();
		
		@Test
	    public void testBackingServer() {
			
	    	GreenRuntime.run(new TestServer(false, 8082, false));
	    	waitForServer("http://127.0.0.1:8082/");
	    	String route = "/testPage";
	    		
			ParallelClientLoadTesterConfig config1 = 
					new ParallelClientLoadTesterConfig(1, 100000, 8082, route, false);
			ParallelClientLoadTesterPayload payload = null;
			
			GreenRuntime.testConcurrentUntilShutdownRequested(
					new ParallelClientLoadTester(config1, payload), timeoutMS);	
								
		}
	    
		
		
		@Test
	    public void testProxyServer() {
			
			ParallelClientLoadTester.LOG_LATENCY_LIMIT = 100_000_000;
			//HTTP1xRouterStage.showHeader = true;
			
			//startup backing server
			GreenRuntime.run(new TestServer(false, 8082, false));
	    	waitForServer("http://127.0.0.1:8082/");
	    	
    	
	    	//startup proxy server in front of backing server
	    	GreenRuntime.run(new GreenProxy("127.0.0.1",8082)); // 8786
	    	waitForServer("http://127.0.0.1:8786/");
	    	
	    	
	    	String route = "/testPage";
	    	int cyclesPerTrack = 10000;
			ParallelClientLoadTesterConfig config1 = 
					new ParallelClientLoadTesterConfig(1, cyclesPerTrack, 8786, route, false);
			ParallelClientLoadTesterPayload payload = null;
			
			GreenRuntime.testConcurrentUntilShutdownRequested(
					new ParallelClientLoadTester(config1, payload), timeoutMS);	
					
			
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
		
//		@Test
//	    public void testBackingFileServerSequential() {
//	    	GreenRuntime.run(new TestFileServer(8083));
//	    	waitForServer("http://127.0.0.1:8083/");
//	    	String route = "/index.html";
//
//	    	
//	    	//GreenRuntime.testUntilShutdownRequested(new TestClientSequential(20000, 8083, route), timeoutMS);	    	
//	    	boolean enableTelemetry = false;
//			GreenRuntime.testConcurrentUntilShutdownRequested(new TestClientSequential(100000, 8083, route, enableTelemetry), timeoutMS);	    	
//
//	    	
//	    	//GreenRuntime.testConcurrentUntilShutdownRequested(
//	    	//		new TestClientBatch(20000, 50, 8083, route, false), timeoutMS);
//	    
//	    	//GreenRuntime.testUntilShutdownRequested(
//	    	//		new TestClientBatch(20000,  50, 8083, route, false), timeoutMS);
//	    	
//	    	//GreenRuntime.testUntilShutdownRequested(
//	    	//		new TestClientParallel(50000, 8, 8083, route, null), timeoutMS);
//	    	
//	    	//GreenRuntime.testConcurrentUntilShutdownRequested(
//	    	//		new TestClientParallel(50000, 8, 8083, route, null), timeoutMS);
//	    	
//	    }
		
}
