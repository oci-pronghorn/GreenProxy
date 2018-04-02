package com.ociweb.behaviors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPRequestReader;
import com.ociweb.gl.api.HeaderWriter;
import com.ociweb.gl.api.ClientHostPortInstance;
import com.ociweb.gl.api.RestListener;
import com.ociweb.gl.api.WaitFor;
import com.ociweb.gl.api.Writable;
import com.ociweb.pronghorn.network.config.HTTPHeader;
import com.ociweb.pronghorn.network.config.HTTPHeaderDefaults;
import com.ociweb.pronghorn.pipe.ChannelReader;
import com.ociweb.pronghorn.struct.BStructFieldVisitor;
import com.ociweb.pronghorn.struct.BStructSchema;

public class ListenerBehavior implements RestListener {
    private final StringBuilder path = new StringBuilder();
    private final StringBuilder headers = new StringBuilder();

    private final String routingTopic;
    private final GreenCommandChannel relayRequestChannel;
    private final ClientHostPortInstance session;
    private static final Logger logger = LoggerFactory.getLogger(ListenerBehavior.class);
    
    
    public ListenerBehavior(String host, int port, GreenRuntime runtime, final ClientHostPortInstance session, String routingTopic) {
        
    	this.session = session;
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


    	
        path.setLength(0);
        httpRequestReader.getRoutePath(path);
        assert(path.length()>0) : "bad path";
        //System.err.println("proxy call to route:"+route);
        

        headers.setLength(0);
    
        switch (httpRequestReader.getVerb()) {
            case GET:
                if (!relayRequestChannel.httpGet(session, path, (w)->{ 
                	headersForProxiedRequest(httpRequestReader,w);
                	})) {
                	assert(false): "should not happen since we checked for room already";
                }
                logger.trace("Proxy has made GET call to {} {} with headers {}",session,path,headers);
                
            case HEAD:
                break;
            case POST:
            	Writable payload = writer -> httpRequestReader.openPayloadData(
            			reader -> reader.readInto(writer, reader.available()));
            	
                if (!relayRequestChannel.httpPost(session, path, (w)->{
                	headersForProxiedRequest(httpRequestReader,w);
                }, payload)) {
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

    private void headersForProxiedRequest(HTTPRequestReader httpRequestReader, HeaderWriter writer) {
    	
    	httpRequestReader.structured().visit(HTTPHeader.class, (header,reader) -> {
				  if (   (header != HTTPHeaderDefaults.HOST)
			            	&& (header != HTTPHeaderDefaults.CONNECTION)	){
			            	                
			            	writer.write((HTTPHeader)header,
			            			     httpRequestReader.getSpec(), 
			            			     reader);
		           }
		});
	
    }
}
