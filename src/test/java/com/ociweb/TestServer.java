package com.ociweb;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.pronghorn.network.config.HTTPContentTypeDefaults;

public class TestServer implements GreenApp {

	private final int port;
	
	public TestServer(int port) {
		this.port = port;
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
		builder.enableServer(false, false, "127.0.0.1",port);
		builder.defineRoute("/testPage");
		
		builder.limitThreads(2);
		
		//builder.enableTelemetry();
		
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {
		
		final GreenCommandChannel cmd = runtime.newCommandChannel(NET_RESPONDER);
		
		runtime.addRestListener((r)->{
			
			
			return cmd.publishHTTPResponse(r, 200, HTTPContentTypeDefaults.TXT, (w)->{
				w.append("exampleResponse");
			});
			
			//TODO: if we always return true here the sequence numbers will get skipped causing a hang
			//      how can we detect this error and return a helpful message...
			
			//return true;
		}).includeAllRoutes();
	}
}
