package com.wps.yundoc.auth.infrastructure;

import com.wps.yundoc.businesssystem.domain.ApiCode;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * CapabilityRoutePolicy component.
 *
 * @author WPS
 */
@Component
@SuppressWarnings("java:S1075")
public class CapabilityRoutePolicy {

    private static final String USER_FILE_PATH = "/api/v1/user/files/";

    private final List<CapabilityRouteRule> rules = Arrays.asList(
            exact(HttpMethod.POST, "/api/v1/app/previews", ApiCode.APP_PREVIEW_CREATE),
            exact(HttpMethod.GET, "/api/v1/wps/oauth/authorize-url", ApiCode.USER_FILES_LIST),
            exact(HttpMethod.GET, "/api/v1/user/files", ApiCode.USER_FILES_LIST),
            suffix(HttpMethod.PATCH, USER_FILE_PATH, "/name", ApiCode.USER_FILES_RENAME),
            suffix(HttpMethod.POST, USER_FILE_PATH, "/download-url", ApiCode.USER_FILES_DOWNLOAD),
            suffix(HttpMethod.PATCH, "/api/v1/user/folders/", "/name", ApiCode.USER_FOLDERS_RENAME),
            exact(HttpMethod.POST, "/api/v1/user/files", ApiCode.USER_FILES_CREATE),
            suffix(HttpMethod.POST, USER_FILE_PATH, "/save-as", ApiCode.USER_FILES_SAVE_AS),
            suffix(HttpMethod.POST, USER_FILE_PATH, "/view-url", ApiCode.USER_FILES_VIEW),
            prefix(HttpMethod.DELETE, USER_FILE_PATH, ApiCode.USER_FILES_DELETE),
            suffix(HttpMethod.PUT, USER_FILE_PATH, "/content", ApiCode.USER_FILES_UPDATE));

    public Optional<String> resolve(HttpServletRequest request) {
        return rules.stream()
                .filter(rule -> rule.matches(request))
                .map(CapabilityRouteRule::apiCode)
                .findFirst();
    }

    private CapabilityRouteRule exact(HttpMethod method, String path, ApiCode apiCode) {
        return new CapabilityRouteRule(method, path, null, RouteMatchType.EXACT, apiCode.getCode());
    }

    private CapabilityRouteRule prefix(HttpMethod method, String path, ApiCode apiCode) {
        return new CapabilityRouteRule(method, path, null, RouteMatchType.PREFIX, apiCode.getCode());
    }

    private CapabilityRouteRule suffix(HttpMethod method, String path, String suffix, ApiCode apiCode) {
        return new CapabilityRouteRule(method, path, suffix, RouteMatchType.SUFFIX, apiCode.getCode());
    }
}
