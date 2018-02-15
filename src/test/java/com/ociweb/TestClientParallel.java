package com.ociweb;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenAppParallel;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPSession;
import com.ociweb.pronghorn.stage.scheduling.ElapsedTimeRecorder;

public class TestClientParallel implements GreenAppParallel {

	private final int port;
	private final String host = "127.0.0.1";
	private final byte[] testValue;
	private final int cycles;
	private final int parallel;
	private int instance = 0;
	private final int countPerTrack;
	private final String route;
	private static final Logger logger = LoggerFactory.getLogger(TestClientParallel.class);
	
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
		builder.enableTelemetry(8099);
	
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {		
	}

	@Override
	public void declareParallelBehavior(GreenRuntime runtime) {
			
		//immutable
		final HTTPSession session = new HTTPSession(host,port,instance++);
		
		
		final SenderFields sf = new SenderFields(countPerTrack);
		final ReceiverFields rf = new ReceiverFields(countPerTrack);
		runtime.addResponseListener((r)->{

		//	System.err.println(rf.countDownReceived);
			if (--rf.countDownReceived>0) {
				
				rf.receivedTimes[rf.callTimeTail++] = System.nanoTime();
				if (null != testValue) {
					r.openPayloadData((c)->{
							if (!c.equalBytes(testValue)) {
								throw new RuntimeException("Unexpected Data");
						}
					});
				}
			} else {
				if (--rf.countDownReceived == -1) {
					//synchronized(host) {
						
						//System.err.println("instance "+instance);
					    ElapsedTimeRecorder hist = new ElapsedTimeRecorder();
					
						
						int c = sf.sentTimes.length;
						while(--c>=0) {
							long value = rf.receivedTimes[c] - sf.sentTimes[c];
							if (value>0) {
								ElapsedTimeRecorder.record(hist, value);
							}
						}
						
						
						if (--instance==0) {
							
							hist.report(System.out);
							
							runtime.shutdownRuntime();
						}
					//}					
					
				}
				
			
				
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
							sf.sentTimes[sf.callTimeHead++] = System.nanoTime();
							
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
