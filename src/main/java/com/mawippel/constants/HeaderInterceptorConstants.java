package com.mawippel.constants;

import io.grpc.Context;
import io.grpc.Metadata;

import java.util.Map;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

// This class exists to hold a single instantiation of `Context.key(...)` and `Metadata.Key.of(...)` because the gRPC
// `Context` matches on a reference to an instance, not on a String value.
public class HeaderInterceptorConstants {

    public static final String AUTHORIZATION = "Authorization";
    public static final String TENANT_ID = "tenant-id";
    public static final String DEVICE_ID = "device-id";
    public static final String MODEL = "model";
    public static final String UDI = "udi";
    public static final String SCOPE = "scope";

    public static final Context.Key<Map<String, String>> HEADERS_CONTEXT = Context.key("grpc-stream-headers");

    public static final Metadata.Key<String> AUTHORIZATION_HEADER_KEY = Metadata.Key.of(AUTHORIZATION, ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> MODEL_HEADER_KEY = Metadata.Key.of(MODEL, ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> UDI_HEADER_KEY = Metadata.Key.of(UDI, ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> SCOPE_HEADER_KEY = Metadata.Key.of(SCOPE, ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> DEVICE_ID_HEADER_KEY = Metadata.Key.of(DEVICE_ID, ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> TENANT_ID_HEADER_KEY = Metadata.Key.of(TENANT_ID, ASCII_STRING_MARSHALLER);

}
