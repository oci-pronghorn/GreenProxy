package com.ociweb.behaviors;

import com.ociweb.gl.api.*;

public class ListenerBehavior implements RestListener {
    private final StringBuilder route = new StringBuilder();
    private final StringBuilder headers = new StringBuilder();
    private final int responseRoutingId;
    private final String routingTopic;
    private final GreenCommandChannel relayRequestChannel;
    private final HTTPSession session;

    public ListenerBehavior(String host, int port, GreenRuntime runtime, int responseRoutingId, String routingTopic) {
        this.session = new HTTPSession(host, port, 42);
        this.responseRoutingId = responseRoutingId;
        this.routingTopic = routingTopic;
        this.relayRequestChannel = runtime.newCommandChannel(NET_REQUESTER | DYNAMIC_MESSAGING);
    }

    @Override
    public boolean restRequest(HTTPRequestReader httpRequestReader) {
        httpRequestReader.getRoutePath(route);
        httpRequestReader.headers(headers);

        relayRequestChannel.publishTopic(routingTopic, writer -> {
            writer.writeLong(httpRequestReader.getRequestContext());
            writer.writeLong(httpRequestReader.getSequenceCode());
        }, WaitFor.All);


        Writable payload = writer -> {
            httpRequestReader.openPayloadData(reader -> {
                reader.readInto(writer, reader.available());
            });
        };

        switch (httpRequestReader.getVerb()) {
            case GET:
                relayRequestChannel.httpGet(session, route, headers, responseRoutingId);
                break;
            case HEAD:
                break;
            case POST:
                relayRequestChannel.httpPost(session, route, headers, payload);
                break;
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
}
