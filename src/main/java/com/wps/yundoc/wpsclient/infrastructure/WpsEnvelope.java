package com.wps.yundoc.wpsclient.infrastructure;

interface WpsEnvelope<T> {

    Integer getCode();

    T getData();
}
