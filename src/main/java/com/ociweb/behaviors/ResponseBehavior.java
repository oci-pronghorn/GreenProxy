package com.ociweb.behaviors;

import com.ociweb.gl.api.GreenCommandChannel;
import com.ociweb.gl.api.GreenRuntime;
import com.ociweb.gl.api.HTTPResponder;
import com.ociweb.gl.api.HTTPResponseListener;
import com.ociweb.gl.api.HTTPResponseReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.gl.api.Writable;
import com.ociweb.pronghorn.network.config.HTTPHeader;
import com.ociweb.pronghorn.network.http.HeaderWritable;
import com.ociweb.pronghorn.pipe.ChannelReader;

public class ResponseBehavior implements PubSubListener, HTTPResponseListener {
    private GreenCommandChannel responseRelayChannel;
    private final HTTPResponder httpResponder;
  

    public ResponseBehavior(GreenRuntime runtime) {
        this.responseRelayChannel = runtime.newCommandChannel();

        this.httpResponder = new HTTPResponder(responseRelayChannel, 256 * 1024);
    }

    @Override
    public boolean message(CharSequence charSequence, ChannelReader channelReader) {
        return httpResponder.readReqesterData(channelReader);

    }

    @Override
    public boolean responseHTTP(HTTPResponseReader responseReader) {
    	assert(responseReader.isStructured());
    	
    	assert(404!=responseReader.statusCode()) : "did not find URL";
  
     
        HeaderWritable headers = (w)->{
			
		    responseReader.structured().visit(HTTPHeader.class, (header,reader)->{
		    	
		    	//w.write(header, header.writeValue(target, httpSpec, responseReader));
		    	//header.writeValue(w, responseReader.getSpec(), responseReader);
		    	w.write(header, responseReader.getSpec(), responseReader);
			   
		    });		    
		};
		
		Writable payload = writer -> responseReader.openPayloadData(
				reader -> reader.readInto(writer, reader.available()));
		
		return httpResponder.respondWith(!responseReader.isEndOfResponse(), 
        		headers,
        		payload);
    
    }
}
