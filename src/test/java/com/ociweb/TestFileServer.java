package com.ociweb;

import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.pronghorn.network.config.HTTPContentTypeDefaults;

public class TestFileServer implements GreenApp {

	private final int port;
	
	public TestFileServer(int port) {
		this.port = port;
	}
	
	@Override
	public void declareConfiguration(Builder builder) {
		builder.enableServer(false, false, "127.0.0.1",port);
		builder.limitThreads(8);
		
		//builder.enableTelemetry();
		
	}

	@Override
	public void declareBehavior(GreenRuntime runtime) {
		
		runtime.addFileServer("./src/test/resources/site/index.html").includeAllRoutes();
	
	}
}
