package com.ociweb.behaviors;

import com.ociweb.gl.api.*;
import com.ociweb.pronghorn.network.config.HTTPHeader;
import com.ociweb.pronghorn.network.config.HTTPHeaderDefaults;
import com.ociweb.pronghorn.network.config.HTTPSpecification;
import com.ociweb.pronghorn.pipe.ChannelReader;
import com.ociweb.pronghorn.util.AppendableProxy;
import com.ociweb.pronghorn.util.Appendables;

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
        headers.setLength(0);
        route.setLength(0);

        httpRequestReader.getRoutePath(route);

        prepareHeadersForProxiedRequest(httpRequestReader, headers);

        relayRequestChannel.publishTopic(routingTopic, httpRequestReader::handoff, WaitFor.All);

        Writable payload = writer -> httpRequestReader.openPayloadData(reader -> reader.readInto(writer, reader.available()));

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

    private Appendable prepareHeadersForProxiedRequest(HTTPRequestReader httpRequestReader, Appendable destination) {

        AppendableProxy appendableProxy = Appendables.proxy(destination);
        HTTPSpecification spec = httpRequestReader.getSpec();

        httpRequestReader.visitHeaders((HTTPHeader header, ChannelReader channelReader) ->  {
            if(header != HTTPHeaderDefaults.HOST){
                appendableProxy.append(header.writingRoot());
                header.writeValue(appendableProxy, spec, channelReader);
                appendableProxy.append("\r\n");
            }
        });

        return destination;
    }
}
