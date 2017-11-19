package com.ociweb;

import com.ociweb.behaviors.ListenerBehavior;
import com.ociweb.behaviors.ResponseBehavior;
import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPServerConfig;

public class GreenProxy implements GreenApp
{
	private final String routingTopic = "routingTopic";

	private final String host;
	private final int port;
	
	private final int proxyPort = 8786;
	
	public GreenProxy() {
		this("localhost", 9080);
	}
	
	public GreenProxy(String host, int port) {		
		this.host = host;
		this.port = port;
	}
	
    @Override
    public void declareConfiguration(Builder builder) {
    	HTTPServerConfig conf = builder.useHTTP1xServer(proxyPort)
    			.setHost("127.0.0.1")
    			.useInsecureServer();
    			
        builder.useInsecureNetClient();
        
        //builder.enableTelemetry();
                
    }

    @Override
    public void declareBehavior(GreenRuntime runtime) {
        int responseRoutingId = runtime.addResponseListener(
        				new ResponseBehavior(runtime))
        						.addSubscription(routingTopic).getId();
        
        runtime.addRestListener(
        				new ListenerBehavior(host, port, runtime, responseRoutingId, routingTopic))
        						.includeAllRoutes();
        
    }
}
