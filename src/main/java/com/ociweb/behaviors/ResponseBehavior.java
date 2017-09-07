package com.ociweb.behaviors;

import com.ociweb.gl.api.*;
import com.ociweb.pronghorn.network.config.HTTPContentTypeDefaults;
import com.ociweb.pronghorn.pipe.BlobReader;

import static com.ociweb.pronghorn.network.ServerCoordinator.END_RESPONSE_MASK;

public class ResponseBehavior implements PubSubListener, HTTPResponseListener {
    private GreenCommandChannel responseRelayChannel;
    private long connectionId = 0;
    private long seqenceCode = 0;

    public ResponseBehavior(GreenRuntime runtime) {
        this.responseRelayChannel = runtime.newCommandChannel(NET_RESPONDER);
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader blobReader) {
        this.connectionId = blobReader.readLong();
        this.seqenceCode = blobReader.readLong();
        return true;
    }

    @Override
    public boolean responseHTTP(HTTPResponseReader responseReader) {
        Writable payload = writer -> {
            responseReader.openPayloadData(reader -> {
                reader.readInto(writer, reader.available());
            });
        };

        return responseRelayChannel.publishHTTPResponse(
                connectionId,
                seqenceCode,
                responseReader.statusCode(),
                END_RESPONSE_MASK,
                responseReader.contentType(),
                payload);
    }
}
