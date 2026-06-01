package com.wps.yundoc.credential.application;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.credential.domain.WpsUserToken;
import com.wps.yundoc.credential.infrastructure.LocalOauthStateCache;
import com.wps.yundoc.credential.infrastructure.LocalWpsUserTokenCache;
import com.wps.yundoc.credential.infrastructure.WpsCredentialProperties;
import com.wps.yundoc.credential.infrastructure.WpsUserAuthorizationProperties;
import com.wps.yundoc.wpsclient.application.WpsAuthorizationClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

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

    @Test
    void concurrentRefreshDoesNotRemoveTokenRefreshedByFirstRequest() throws Exception {
        LocalWpsUserTokenCache tokenCache = new LocalWpsUserTokenCache(new WpsCredentialProperties());
        tokenCache.put("user-001", new WpsUserToken(
                "old-access",
                OffsetDateTime.now().plusSeconds(1),
                "old-refresh",
                OffsetDateTime.now().plusDays(1),
                "bearer"));
        BlockingRefreshAuthorizationClient authorizationClient = new BlockingRefreshAuthorizationClient();
        WpsUserAuthorizationService service = service(tokenCache, authorizationClient);
        AtomicReference<WpsUserToken> firstResult = new AtomicReference<>();
        AtomicReference<WpsUserToken> secondResult = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        Thread first = new Thread(() -> firstResult.set(requireUserToken(service)), "first-refresh");
        Thread second = new Thread(() -> {
            try {
                secondResult.set(requireUserToken(service));
            } catch (Throwable ex) {
                secondFailure.set(ex);
            }
        }, "second-refresh");

        first.start();
        authorizationClient.awaitRefreshStarted();
        second.start();
        waitUntilBlocked(second);
        authorizationClient.allowRefresh();
        first.join(TimeUnit.SECONDS.toMillis(5));
        second.join(TimeUnit.SECONDS.toMillis(5));

        assertThat(secondFailure.get()).isNull();
        assertThat(firstResult.get().getAccessToken()).isEqualTo("new-access");
        assertThat(secondResult.get().getAccessToken()).isEqualTo("new-access");
        assertThat(authorizationClient.refreshCalls()).isEqualTo(1);
        assertThat(tokenCache.get("user-001").get().getAccessToken()).isEqualTo("new-access");
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
        return service(new LocalWpsUserTokenCache(new WpsCredentialProperties()), authorizationClient());
    }

    private WpsUserAuthorizationService service(
            LocalWpsUserTokenCache tokenCache,
            WpsAuthorizationClient authorizationClient) {
        return new WpsUserAuthorizationService(
                tokenCache,
                new LocalOauthStateCache(new WpsUserAuthorizationProperties()),
                authorizationClient,
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
                return new WpsUserToken(
                        "user-token",
                        OffsetDateTime.now().plusMinutes(30),
                        "refresh-token",
                        OffsetDateTime.now().plusDays(365),
                        "bearer");
            }

            @Override
            public WpsUserToken refreshToken(String refreshToken) {
                return new WpsUserToken(
                        "refreshed-user-token",
                        OffsetDateTime.now().plusMinutes(30),
                        "refreshed-refresh-token",
                        OffsetDateTime.now().plusDays(365),
                        "bearer");
            }
        };
    }

    private WpsUserToken requireUserToken(WpsUserAuthorizationService service) {
        return service.requireUserToken("user-001", "biz-001", "client-001");
    }

    private void waitUntilBlocked(Thread thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (thread.getState() == Thread.State.BLOCKED) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10L));
        }
        throw new AssertionError("second refresh thread did not block on refresh lock");
    }

    private static class BlockingRefreshAuthorizationClient implements WpsAuthorizationClient {

        private final CountDownLatch refreshStarted = new CountDownLatch(1);
        private final CountDownLatch allowRefresh = new CountDownLatch(1);
        private final AtomicInteger refreshCalls = new AtomicInteger();

        @Override
        public String authorizeUrl(String state) {
            return "https://wps.test/oauth/authorize?state=" + state;
        }

        @Override
        public WpsUserToken exchangeCode(String code) {
            return new WpsUserToken(
                    "user-token",
                    OffsetDateTime.now().plusMinutes(30),
                    "refresh-token",
                    OffsetDateTime.now().plusDays(365),
                    "bearer");
        }

        @Override
        public WpsUserToken refreshToken(String refreshToken) {
            refreshStarted.countDown();
            int calls = refreshCalls.incrementAndGet();
            if (calls > 1) {
                throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR);
            }
            awaitAllowed();
            return new WpsUserToken(
                    "new-access",
                    OffsetDateTime.now().plusMinutes(30),
                    "new-refresh",
                    OffsetDateTime.now().plusDays(365),
                    "bearer");
        }

        void awaitRefreshStarted() throws InterruptedException {
            assertThat(refreshStarted.await(5, TimeUnit.SECONDS)).isTrue();
        }

        void allowRefresh() {
            allowRefresh.countDown();
        }

        int refreshCalls() {
            return refreshCalls.get();
        }

        private void awaitAllowed() {
            try {
                allowRefresh.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting to refresh", ex);
            }
        }
    }
}
