package com.ociweb;

import com.ociweb.behaviors.ListenerBehavior;
import com.ociweb.behaviors.ResponseBehavior;
import com.ociweb.gl.api.Builder;
import com.ociweb.gl.api.GreenApp;
import com.ociweb.gl.api.GreenRuntime;

public class GreenProxy implements GreenApp
{
    @Override
    public void declareConfiguration(Builder builder) {
        builder.enableServer(false, 8786);
        builder.useInsecureNetClient();
    }

    @Override
    public void declareBehavior(GreenRuntime runtime) {
        String routingTopic = "routingTopic";
        ResponseBehavior responder = new ResponseBehavior(runtime);
        int responseRoutingId = runtime.addResponseListener(responder).addSubscription(routingTopic).getId();
        ListenerBehavior listen = new ListenerBehavior("localhost", 9080, runtime, responseRoutingId, routingTopic);
        runtime.addRestListener(listen).includeAllRoutes();
    }
}
