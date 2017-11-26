package com.ociweb;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenAppParallel;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPSession;
import com.ociweb.pronghorn.util.Appendables;

public class TestClientParallel implements GreenAppParallel {

	private final int port;
	private final String host = "127.0.0.1";
	private final byte[] testValue;
	private final int cycles;
	private final int parallel;
	private int instance = 0;
	private final int countPerTrack;
	private final String route;
	
	public TestClientParallel(int cycles, int parallel, int port, String route, byte[] testValue) {
		this.port = port;
		this.testValue = testValue;
		this.cycles = cycles;
		this.parallel = parallel;
		this.countPerTrack = cycles/parallel;
		this.route = route;
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
		
		builder.useInsecureNetClient();
		builder.parallelism(parallel);
		builder.setTimerPulseRate(1);
		//builder.enableTelemetry(8099);
		//builder.limitThreads(20);
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {		
	}

	@Override
	public void declareParallelBehavior(GreenRuntime runtime) {
		
		final int inFlightBits = 8;
		final int inFlightSize = 1<<inFlightBits;
		final int inFlightMask = inFlightSize-1;
		final long[] callTime = new long[inFlightSize];
		
		final SenderFields sf = new SenderFields(countPerTrack);		
		final ReceiverFields rf = new ReceiverFields(countPerTrack);
				
		HTTPSession session = new HTTPSession(host,port,instance++);
		
		
		runtime.addResponseListener((r)->{
			long duration = System.nanoTime()-callTime[inFlightMask & (int)rf.callTimeTail++];
			
			rf.totalTime += duration;
			
			if (null != testValue) {
				r.openPayloadData((c)->{
						if (!c.equalBytes(testValue)) {
							throw new RuntimeException("Unexpected Data");
					}
				});
			}
			
			if (--rf.countDownReceived<=0) {
				synchronized(host) {
					System.out.println();
					Appendables.appendNearestTimeUnit(System.out, rf.totalTime/countPerTrack, " latency \n");
					System.out.println();
				}
				runtime.shutdownRuntime();
			}
			return true;
		}).includeHTTPSession(session);
			
		
		///TODO: we need to know how many sessions will be used??
		GreenCommandChannel cmd1 = runtime.newCommandChannel(NET_REQUESTER);
		cmd1.ensureHTTPClientRequesting(2, 30);
		
		runtime.addTimePulseListener((t,i)->{
			
				if (sf.countDownSent>0) {
					
						if (cmd1.httpGet(session, route)) {
							//NOTE: these already have time for get calls sitting
							//      in the outgoing pipe, if that pipe is long
							callTime[inFlightMask & (int)sf.callTimeHead++] = System.nanoTime();
							sf.countDownSent--;
						}
				
				} else {
					if (--sf.countDownSent==-1) {
						System.out.println("Finished Sending to "+session);
					}
				}
		});
		
	}

}
