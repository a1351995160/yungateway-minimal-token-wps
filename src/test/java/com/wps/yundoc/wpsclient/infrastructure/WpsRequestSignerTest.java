package com.wps.yundoc.wpsclient.infrastructure;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class WpsRequestSignerTest {

    private static final String ACCESS_KEY = "AK123456";
    private static final String SECRET_KEY = "sk098765";
    private static final String CONTENT_TYPE = "application/json";
    private static final String KSO_DATE = "Mon, 02 Jan 2006 15:04:05 GMT";

    @Test
    void kso1SignMatchesOfficialGetExample() {
        WpsRequestSigner signer = new WpsRequestSigner(ACCESS_KEY, SECRET_KEY);

        WpsSignatureHeaders headers = signer.kso1Sign(
                "GET",
                "/v7/test?key=value",
                CONTENT_TYPE,
                KSO_DATE,
                new byte[0]);

        assertThat(headers.getValues())
                .containsEntry(WpsRequestSigner.KSO_DATE_HEADER, KSO_DATE)
                .containsEntry(
                        WpsRequestSigner.KSO_AUTHORIZATION_HEADER,
                        "KSO-1 AK123456:ce8df66877175e5198c8ea1362ffddf82e4941c6f25a4ca205a1ad09d0faaf03");
    }

    @Test
    void kso1SignMatchesOfficialPostBodyExample() {
        WpsRequestSigner signer = new WpsRequestSigner(ACCESS_KEY, SECRET_KEY);

        WpsSignatureHeaders headers = signer.kso1Sign(
                "POST",
                "/v7/test/body",
                CONTENT_TYPE,
                KSO_DATE,
                "{\"key\": \"value\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(headers.getValues())
                .containsEntry(
                        WpsRequestSigner.KSO_AUTHORIZATION_HEADER,
                        "KSO-1 AK123456:c46e6c988130818ecba2484d51ac685948fbbef6814602c7874d6bfc41dc17b3");
    }

    @Test
    void wps3SignBuildsReusableLegacyHeaders() {
        WpsRequestSigner signer = new WpsRequestSigner(ACCESS_KEY, SECRET_KEY);

        WpsSignatureHeaders headers = signer.wps3Sign(
                "/v7/test/body",
                CONTENT_TYPE,
                KSO_DATE,
                "{\"key\": \"value\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(headers.getValues())
                .containsEntry(WpsRequestSigner.WPS3_DATE_HEADER, KSO_DATE)
                .containsEntry(WpsRequestSigner.WPS3_CONTENT_MD5_HEADER, "iLrJXzFSjROgcsBfKhzzcQ==")
                .containsEntry(WpsRequestSigner.WPS3_AUTHORIZATION_HEADER, "WPS-3:AK123456:RG+OAFvR9Gd70SnzV59r0WkPmxY=");
    }

    @Test
    void signUsesWpsRfc1123DateWithTwoDigitDay() {
        Clock clock = Clock.fixed(Instant.parse("2006-01-02T15:04:05Z"), ZoneOffset.UTC);
        WpsRequestSigner signer = new WpsRequestSigner(ACCESS_KEY, SECRET_KEY, clock);

        WpsSignatureHeaders headers = signer.sign(WpsRequestSigner.KSO_1, "GET", "/v7/test", CONTENT_TYPE, new byte[0]);

        assertThat(headers.getValues())
                .containsEntry(WpsRequestSigner.KSO_DATE_HEADER, "Mon, 02 Jan 2006 15:04:05 GMT");
    }
}
