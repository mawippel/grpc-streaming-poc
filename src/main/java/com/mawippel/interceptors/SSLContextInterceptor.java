package com.mawippel.interceptors;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSession;

@Slf4j
@Component
public class SSLContextInterceptor implements ServerInterceptor {

    public final static Context.Key<SSLSession> SSL_SESSION_CONTEXT =  Context.key("SSLSession");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        SSLSession sslSession = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
        if (sslSession == null) {
            return next.startCall(call, headers);
        }
        return Contexts.interceptCall(Context.current().withValue(SSL_SESSION_CONTEXT, sslSession), call, headers, next);
    }
}
