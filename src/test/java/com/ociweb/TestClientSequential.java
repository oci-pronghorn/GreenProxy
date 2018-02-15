package com.ociweb;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPSession;
import com.ociweb.pronghorn.util.Appendables;

public class TestClientSequential implements GreenApp {
	
	private final HTTPSession session;
	private int countDown;
	private long callTime;
	
	private long totalTime = 0;
	private final int totalCycles;
    private final String route;
    private final boolean enableTelemetry;
    
    private static final String STARTUP_NAME   = "startup";
    private static final String CALLER_NAME    = "caller";
    private static final String RESPONDER_NAME = "responder";
    private static final String CALL_TOPIC     = "makeCall";
        
	public TestClientSequential(int cycles, int port, String route, boolean enableTelemetry) {
		countDown = cycles;
		totalCycles = cycles;
		session = new HTTPSession("127.0.0.1",port);
		this.route = route;
		this.enableTelemetry = enableTelemetry;
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
		builder.useInsecureNetClient();
		builder.limitThreads(2);
		if (enableTelemetry) {
			builder.enableTelemetry();
		}
		
		//ScriptedNonThreadScheduler.debug = true;
		
		//use private topics
		//builder.definePrivateTopic(STARTUP_NAME, CALLER_NAME, CALL_TOPIC);
		//builder.definePrivateTopic(RESPONDER_NAME, CALLER_NAME, CALL_TOPIC);
		
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {
		
		GreenCommandChannel cmd1 = runtime.newCommandChannel(DYNAMIC_MESSAGING);
		runtime.addStartupListener(STARTUP_NAME, ()->{
			cmd1.publishTopic(CALL_TOPIC);
		});
				
		GreenCommandChannel cmd3 = runtime.newCommandChannel(DYNAMIC_MESSAGING);
		runtime.addResponseListener(RESPONDER_NAME, (r)->{
			long duration = System.nanoTime() - callTime;
				
			totalTime += duration;
			
			if (--countDown>0) {
				return cmd3.publishTopic(CALL_TOPIC);
			} else {
				System.out.println();
				Appendables.appendNearestTimeUnit(System.out, totalTime/totalCycles, " latency on "+session);
				System.out.println();
				
				cmd3.shutdown();
				return true;
			}
		}).includeHTTPSession(session);
		

		GreenCommandChannel cmd2 = runtime.newCommandChannel(NET_REQUESTER);
		runtime.addPubSubListener(CALLER_NAME, (t,p)->{
			callTime = System.nanoTime();
			return cmd2.httpGet(session, route);
		}).addSubscription(CALL_TOPIC);
	
	}

}
