package com.ociweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPSession;
import com.ociweb.gl.api.TimeTrigger;
import com.ociweb.pronghorn.util.Appendables;

public class TestClientParallel implements GreenApp {
	
	private final HTTPSession session;
	private int countDownSent;
	private int countDownReceived;
	
		
	private final int inFlightBits;
	private final int inFlight;
	private final int inFlightMask;
	
	private long[] callTime;
	private long callTimeHead;
	private long callTimeTail;
	
	private final static Logger logger = LoggerFactory.getLogger(TestClientParallel.class);
	
	private long totalTime;
	private final int totalCycles;
	
	private long rateInMS = 1;
	private int multiplier = 4;
	
	public TestClientParallel(int cycles, int port) {
		countDownSent = cycles;
		countDownReceived = cycles;
		
		totalCycles = cycles;
		session = new HTTPSession("127.0.0.1",port);
	
		inFlightBits = 14;
		
		inFlight = 1<<inFlightBits;
		inFlightMask = inFlight-1;
		
		callTime = new long[inFlight];
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
		builder.useInsecureNetClient();
		builder.setTimerPulseRate(rateInMS);
		
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {
		
		GreenCommandChannel cmd3 = runtime.newCommandChannel(DYNAMIC_MESSAGING);
		int id = runtime.addResponseListener((r)->{
			long duration = System.nanoTime()-callTime[inFlightMask & (int)callTimeTail++];
			
			totalTime += duration;
			
			r.openPayloadData((c)->{
		
				if (!c.equalBytes("exampleResponse".getBytes())) {
					throw new RuntimeException("Unexpected Data");
				}
				
			});
			
			if (--countDownReceived<=0) {
				System.out.println();
				Appendables.appendNearestTimeUnit(System.out, totalTime/totalCycles, " latency on "+session+"\n");
				System.out.println();
				cmd3.shutdown();
			}
			return true;
		}).getId();
		
		
		GreenCommandChannel cmd1 = runtime.newCommandChannel(NET_REQUESTER);
		cmd1.ensureHTTPClientRequesting(5000, 30);
		runtime.addTimePulseListener((t,i)->{
			
			int m = multiplier;
			while (--m>=0) {
				if (countDownSent>0) {
					if (callTimeHead-callTimeTail<inFlight) {
						callTime[inFlightMask & (int)callTimeHead++] = System.nanoTime();
						if (cmd1.httpGet(session, "/testPage", id)) {
							countDownSent--;
						} else {
							logger.warn("output queue is full");						
						}
					} else {
						logger.warn("increase inFlightBits value.");
					}
				} else {
					if (--countDownSent==-1) {
						logger.info("Finished Sending");
					}
				}
			}
			
		});
		
		

		

	
	}

}
