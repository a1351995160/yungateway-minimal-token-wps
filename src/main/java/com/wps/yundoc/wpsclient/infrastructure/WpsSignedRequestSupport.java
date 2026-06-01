package com.wps.yundoc.wpsclient.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import org.springframework.http.HttpHeaders;
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
