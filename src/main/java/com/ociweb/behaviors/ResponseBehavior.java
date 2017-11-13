package com.ociweb.behaviors;

import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPResponder;
import com.ociweb.gl.api.HTTPResponseListener;
import com.ociweb.gl.api.HTTPResponseReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.gl.api.Writable;
import com.ociweb.pronghorn.pipe.ChannelReader;

public class ResponseBehavior implements PubSubListener, HTTPResponseListener {
    private GreenCommandChannel responseRelayChannel;
    private final HTTPResponder httpResponder;
    private final StringBuilder headers = new StringBuilder();

    public ResponseBehavior(GreenRuntime runtime) {
        this.responseRelayChannel = runtime.newCommandChannel();
        responseRelayChannel.ensureHTTPServerResponse(500, 1024);
        this.httpResponder = new HTTPResponder(responseRelayChannel, 256 * 1024);
    }

    @Override
    public boolean message(CharSequence charSequence, ChannelReader channelReader) {
        return httpResponder.readReqesterData(channelReader);

    }

    @Override
    public boolean responseHTTP(HTTPResponseReader responseReader) {
    	    	
        headers.setLength(0);
        responseReader.headers(headers);

        Writable payload = writer -> responseReader.openPayloadData(
        		                     reader -> reader.readInto(writer, reader.available()));

        
        return httpResponder.respondWith(!responseReader.isEndOfResponse(), headers.toString(), payload);
    
    }
}
