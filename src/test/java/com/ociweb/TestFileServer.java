package com.ociweb;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenAppParallel;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPServerConfig;
import com.ociweb.pronghorn.network.config.HTTPContentTypeDefaults;

public class TestFileServer implements GreenAppParallel {

	private final int port;
	
	public TestFileServer(int port) {
		this.port = port;
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
    	HTTPServerConfig conf = builder.useHTTP1xServer(port)
    			.setHost("127.0.0.1")
    			.useInsecureServer();
    	
    	conf.setConcurrentChannelsPerDecryptUnit(4);
    	builder.parallelism(4);
		//builder.limitThreads(8);
		//builder.defineRoute("/${path}");
		//builder.enableTelemetry(8091);
		
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {
		
	}

	@Override
	public void declareParallelBehavior(GreenRuntime runtime) {
		runtime.addFileServer("./src/test/resources/site/index.html").includeAllRoutes();
	}

}
