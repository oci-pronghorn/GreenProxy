package com.ociweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPPublishService;
import com.ociweb.gl.api.ClientHostPortInstance;
import com.ociweb.gl.api.ListenerFilter;
import com.ociweb.gl.api.TimeTrigger;
import com.ociweb.pronghorn.util.Appendables;

public class TestClientBatch implements GreenApp {
	
	private final ClientHostPortInstance[] sessions;
	private int countDownSent;
	private int countDownReceived;
	
		
	private final int inFlightBits;
	private final int inFlight;
	private final int inFlightMask;
	
	private long[] callTime;
	private long callTimeHead;
	private long callTimeTail;
	
	private final static Logger logger = LoggerFactory.getLogger(TestClientBatch.class);
	
	private long totalTime;
	private final int totalCycles;
	private final int multiplier;
	private long rateInMS = 1;
	private final String route;
	private final boolean doTest;
	
	public TestClientBatch(int cycles, int multiplier, int port, String route, boolean doTest) {
		countDownSent = cycles;
		countDownReceived = cycles;
		this.route = route;
		totalCycles = cycles;
		this.multiplier = multiplier;
		
		int s = multiplier;
		sessions = new ClientHostPortInstance[s];
		while (--s>=0) {		
			sessions[s] = new ClientHostPortInstance("127.0.0.1",port,s);
		}
		
		this.doTest = doTest;
		inFlightBits = 18;
		
		inFlight = 1<<inFlightBits;
		inFlightMask = inFlight-1;
		
		callTime = new long[inFlight];
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
		builder.useInsecureNetClient();
		builder.setTimerPulseRate(rateInMS);
		
		builder.enableTelemetry(8099);
		builder.limitThreads(4);
	}

	@Override
	public void declareBehavior(final GreenRuntime runtime) {
						
		ListenerFilter respConf = runtime.addResponseListener((r)->{
			
			long startTime = callTime[inFlightMask & (int)callTimeTail++];
			if (0==startTime) {
				throw new UnsupportedOperationException();
			}
			long duration = System.nanoTime()-startTime;
			
			totalTime += duration;
			
			r.openPayloadData((c)->{
				if (doTest) {
					if (!c.equalBytes("exampleResponse".getBytes())) {
						throw new RuntimeException("Unexpected Data");
					}
				}
				
			});
			
			if (--countDownReceived<=0) {
				System.out.println();
				Appendables.appendNearestTimeUnit(System.out, totalTime/totalCycles, " latency \n");
				System.out.println();
				runtime.shutdownRuntime();
			}
			return true;
		});
		
		int x = multiplier;
		while (--x >= 0) {
			respConf.includeHTTPSession(sessions[x]);//TODO: need method for accept all..
		}
		////
		
		GreenCommandChannel cmd1 = runtime.newCommandChannel();
		HTTPPublishService clientService = cmd1.newHTTPClientService(12, 30);
		
		runtime.addTimePulseListener((t,i)->{

			int m = multiplier;
			while (--m >= 0) {
				if (countDownSent>0) {
					if (callTimeHead-callTimeTail<inFlight) {
						if (clientService.httpGet(sessions[m], route)) {
							//NOTE: these already have time for get calls sitting
							//      in the outgoing pipe, if that pipe is long
							callTime[inFlightMask & (int)callTimeHead++] = System.nanoTime();
							countDownSent--;
						}
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
