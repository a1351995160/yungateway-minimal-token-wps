package com.wps.yundoc.common.context;

import java.util.Optional;

public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext requestContext) {
        CONTEXT.set(requestContext);
    }

    public static Optional<RequestContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static Optional<String> currentRequestId() {
        return current().map(RequestContext::getRequestId);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

