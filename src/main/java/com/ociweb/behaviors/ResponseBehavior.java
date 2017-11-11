package com.ociweb.behaviors;

import com.ociweb.gl.api.*;
import com.ociweb.pronghorn.pipe.*;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class ResponseBehavior implements PubSubListener, HTTPResponseListener {
    private GreenCommandChannel responseRelayChannel;
    private GraphManager graphManager;
    private final HTTPResponder httpResponder;
    private final StringBuilder headers = new StringBuilder();
    private final GreenRuntime runtime;

    public ResponseBehavior(GreenRuntime runtime) {
        this.runtime = runtime;
        this.responseRelayChannel = runtime.newCommandChannel();
        graphManager = GreenRuntime.getGraphManager(runtime);
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

        Writable payload = writer -> responseReader.openPayloadData(reader -> reader.readInto(writer, reader.available()));

        return httpResponder.respondWith(false, headers.toString(), payload);
    }
}
