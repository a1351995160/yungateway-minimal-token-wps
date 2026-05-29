package com.wps.yundoc.credential.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.credential.infrastructure.LocalOAuthStateCache;
import com.wps.yundoc.credential.infrastructure.LocalWpsUserTokenCache;
import com.wps.yundoc.credential.infrastructure.WpsCredentialProperties;
import com.wps.yundoc.credential.infrastructure.WpsUserAuthorizationProperties;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WpsUserAuthorizationServiceTest {

    @Test
    void rejectsBlankCodeWithoutConsumingState() {
        WpsUserAuthorizationService service = service();
        YundocException reauth = reauthRequired(service);
        String state = stateFrom(reauth);

        assertThatThrownBy(() -> service.handleCallback("", state))
                .isInstanceOf(YundocException.class)
                .hasFieldOrPropertyWithValue("errorCode", YundocErrorCode.VALIDATION_FAILED);

        service.handleCallback("ok-code", state);

        assertThat(service.requireUserToken("user-001", "biz-001").getAccessToken()).isEqualTo("user-token");
    }

    private YundocException reauthRequired(WpsUserAuthorizationService service) {
        try {
            service.requireUserToken("user-001", "biz-001");
        } catch (YundocException ex) {
            return ex;
        }
        throw new AssertionError("reauth must be required");
    }

    private String stateFrom(YundocException exception) {
        URI uri = URI.create(exception.getDetails().get("authorizeUrl").toString());
        return uri.getQuery().replaceFirst("^.*state=([^&]+).*$", "$1");
    }

    private WpsUserAuthorizationService service() {
        return new WpsUserAuthorizationService(
                new LocalWpsUserTokenCache(new WpsCredentialProperties()),
                new LocalOAuthStateCache(new WpsUserAuthorizationProperties()),
                authorizationClient(),
                new WpsUserAuthorizationProperties());
    }

    private WpsAuthorizationClient authorizationClient() {
        return new WpsAuthorizationClient() {
            @Override
            public String authorizeUrl(String state) {
                return "https://wps.test/oauth/authorize?state=" + state;
            }

            @Override
            public WpsUserToken exchangeCode(String code) {
                if (code.trim().isEmpty()) {
                    throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
                }
                return new WpsUserToken("user-token", OffsetDateTime.now().plusMinutes(30));
            }
        };
    }
}
