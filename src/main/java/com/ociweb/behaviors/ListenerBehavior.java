package com.ociweb.behaviors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPRequestReader;
import com.ociweb.gl.api.HTTPSession;
import com.ociweb.gl.api.RestListener;
import com.ociweb.gl.api.WaitFor;
import com.ociweb.gl.api.Writable;
import com.ociweb.pronghorn.network.config.HTTPHeader;
import com.ociweb.pronghorn.network.config.HTTPHeaderDefaults;
import com.ociweb.pronghorn.pipe.ChannelReader;

public class ListenerBehavior implements RestListener {
    private final StringBuilder route = new StringBuilder();
    private final StringBuilder headers = new StringBuilder();
    private final int responseRoutingId;
    private final String routingTopic;
    private final GreenCommandChannel relayRequestChannel;
    private final HTTPSession session;
    private static final Logger logger = LoggerFactory.getLogger(ListenerBehavior.class);

    public ListenerBehavior(String host, int port, GreenRuntime runtime, int responseRoutingId, String routingTopic) {
        this.session = new HTTPSession(host, port, 0);
        this.responseRoutingId = responseRoutingId;
        this.routingTopic = routingTopic;
        this.relayRequestChannel = runtime.newCommandChannel(NET_REQUESTER | DYNAMIC_MESSAGING);
        
        //TODO: need better stack trace to find this chnannel when it is too small..
        relayRequestChannel.ensureHTTPClientRequesting(500, 1024);
    }

    @Override
    public boolean restRequest(HTTPRequestReader httpRequestReader) {

    	if (!relayRequestChannel.hasRoomFor(2)
    		|| !relayRequestChannel.hasRoomForHTTP(1)) {
    		return false;
    	}
    	if (!relayRequestChannel.publishTopic(routingTopic, httpRequestReader::handoff, WaitFor.All)) {
    		//this will only happen if the messages are backing up but not the go pipes
    		return false; //try again later
    	}

        route.setLength(0);
        httpRequestReader.getRoutePath(route);

        headers.setLength(0);
        prepareHeadersForProxiedRequest(httpRequestReader, headers);
        
        switch (httpRequestReader.getVerb()) {
            case GET:
                if (!relayRequestChannel.httpGet(session, route, headers, responseRoutingId)) {
                	assert(false): "should not happen since we checked for room already";
                }
                logger.trace("Proxy has made GET call to {} {} with headers {}",session,route,headers);
                
            case HEAD:
                break;
            case POST:
            	Writable payload = writer -> httpRequestReader.openPayloadData(
            			reader -> reader.readInto(writer, reader.available()));
            	
                if (!relayRequestChannel.httpPost(session, route, headers, payload, responseRoutingId)) {
                	assert(false): "should not happen since we checked for room already";
                }
                
            case PUT:
                break;
            case DELETE:
                break;
            case TRACE:
                break;
            case OPTIONS:
                break;
            case CONNECT:
                break;
            case PATCH:
                break;
        }
        return true;
    }

    private Appendable prepareHeadersForProxiedRequest(HTTPRequestReader httpRequestReader, StringBuilder destination) {

        httpRequestReader.visitHeaders((HTTPHeader header, ChannelReader channelReader) ->  {
            if (   (header != HTTPHeaderDefaults.HOST)
            	&& (header != HTTPHeaderDefaults.CONNECTION)	){
            	
                destination.append(header.writingRoot());
                header.writeValue(destination, httpRequestReader.getSpec(), channelReader);
                destination.append("\r\n");
            }
        });

        return destination;
    }
}
