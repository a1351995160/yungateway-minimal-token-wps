package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * WpsSignedRequestSupport component.
 *
 * @author WPS
 */
final class WpsSignedRequestSupport {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String EMPTY_BODY_HASH = "";

    private WpsSignedRequestSupport() {
    }

    static byte[] jsonBody(Object payload) {
        try {
            return JSON_MAPPER.writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "WPS request body serialization failed", ex);
        }
    }

    static HttpHeaders signedJsonHeaders(
            WpsClientProperties properties,
            WpsRequestSigner signer,
            String method,
            String absoluteUrl,
            byte[] requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applySignature(headers, signer.sign(
                properties.getSignatureVersion(),
                method,
                requestUri(absoluteUrl),
                MediaType.APPLICATION_JSON_VALUE,
                requestBody));
        return headers;
    }

    static HttpHeaders signedOctetStreamHeaders(
            WpsClientProperties properties,
            WpsRequestSigner signer,
            WpsUploadFileRequest request,
            HttpMethod method) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(request.getSize());
        applySignature(headers, kso1Signature(
                properties,
                signer,
                method.name(),
                request.getStoreRequest().getUrl(),
                request.getSha256()));
        return headers;
    }

    private static WpsSignatureHeaders kso1Signature(
            WpsClientProperties properties,
            WpsRequestSigner signer,
            String method,
            String absoluteUrl,
            String bodySha256) {
        if (WpsRequestSigner.NONE.equalsIgnoreCase(properties.getSignatureVersion())) {
            return new WpsSignatureHeaders(new java.util.LinkedHashMap<>());
        }
        return signer.kso1SignWithBodySha256(
                method,
                requestUri(absoluteUrl),
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)),
                bodySha256 == null ? EMPTY_BODY_HASH : bodySha256);
    }

    static void applySignature(HttpHeaders headers, WpsSignatureHeaders signatureHeaders) {
        for (Map.Entry<String, String> entry : signatureHeaders.getValues().entrySet()) {
            headers.set(entry.getKey(), entry.getValue());
        }
    }

    static String requestUri(String absoluteUrl) {
        try {
            URI uri = new URI(absoluteUrl);
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            if (rawQuery == null) {
                return rawPath;
            }
            return rawPath + "?" + rawQuery;
        } catch (URISyntaxException ex) {
            throw WpsClientSupport.upstreamError(ex);
        }
    }
}
