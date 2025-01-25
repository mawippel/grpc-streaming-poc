package com.mawippel.interceptors;

import com.mawippel.constants.HeaderInterceptorConstants;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.mawippel.constants.HeaderInterceptorConstants.*;

@Slf4j
@Component
public class GrpcHeaderInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        String authorization = emptyIfNull(requestHeaders.get(AUTHORIZATION_HEADER_KEY));
        String model = emptyIfNull(requestHeaders.get(MODEL_HEADER_KEY));
        String udi = emptyIfNull(requestHeaders.get(UDI_HEADER_KEY));
        String scope = emptyIfNull(requestHeaders.get(SCOPE_HEADER_KEY));
        String deviceId = emptyIfNull(requestHeaders.get(DEVICE_ID_HEADER_KEY));

        Context ctx = Context.current()
                .withValue(HeaderInterceptorConstants.HEADERS_CONTEXT,
                        Map.of(AUTHORIZATION, authorization,
                                MODEL, model,
                                UDI, udi,
                                SCOPE, scope,
                                DEVICE_ID, deviceId));

        return Contexts.interceptCall(ctx, call, requestHeaders, next);
    }

    private String emptyIfNull(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
