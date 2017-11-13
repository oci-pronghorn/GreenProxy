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
	
	public TestClientSequential(int cycles, int port) {
		countDown = cycles;
		totalCycles = cycles;
		session = new HTTPSession("127.0.0.1",port);
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
		builder.useInsecureNetClient();
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {
		
		GreenCommandChannel cmd1 = runtime.newCommandChannel(DYNAMIC_MESSAGING);
		runtime.addStartupListener(()->{
			cmd1.publishTopic("makeCall");
		});
		
		GreenCommandChannel cmd3 = runtime.newCommandChannel(DYNAMIC_MESSAGING);
		int id = runtime.addResponseListener((r)->{
			long duration = System.nanoTime()-callTime;
	
			totalTime += duration;
			
			if (--countDown>0) {
				return cmd3.publishTopic("makeCall");
			} else {
				System.out.println();
				Appendables.appendNearestTimeUnit(System.out, totalTime/totalCycles, " latency on "+session);
				System.out.println();
				
				cmd3.shutdown();
				return true;
			}
		}).getId();
		

		GreenCommandChannel cmd2 = runtime.newCommandChannel(NET_REQUESTER);
		runtime.addPubSubListener((t,p)->{
			callTime = System.nanoTime();
			return cmd2.httpGet(session, "/testPage", id);
		}).addSubscription("makeCall");
	
	}

}
