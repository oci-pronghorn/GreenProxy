package com.ociweb.behaviors;

import com.ociweb.gl.api.*;
import com.ociweb.pronghorn.network.config.HTTPContentTypeDefaults;
import com.ociweb.pronghorn.pipe.ChannelReader;

import static com.ociweb.pronghorn.network.ServerCoordinator.END_RESPONSE_MASK;

public class ResponseBehavior implements PubSubListener, HTTPResponseListener {
    private GreenCommandChannel responseRelayChannel;
    private final HTTPResponder httpResponder;
    private final StringBuilder headers = new StringBuilder();

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
        Writable payload = writer -> responseReader.openPayloadData(reader -> reader.readInto(writer, reader.available()));
        responseReader.headers(headers);
        return httpResponder.respondWith(false, headers.toString(), payload);
    }
}
