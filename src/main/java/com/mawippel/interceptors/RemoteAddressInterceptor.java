package com.mawippel.interceptors;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.SocketAddress;

@Slf4j
@Component
public class RemoteAddressInterceptor implements ServerInterceptor {

    public final static Context.Key<SocketAddress> REMOTE_ADDR_CONTEXT = Context.key("remote-addr");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        SocketAddress socketAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (socketAddress == null) {
            return next.startCall(call, headers);
        }
        return Contexts.interceptCall(Context.current().withValue(REMOTE_ADDR_CONTEXT, socketAddress), call, headers, next);
    }
}
