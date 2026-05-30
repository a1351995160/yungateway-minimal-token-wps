package com.wps.yundoc.capability.userfile.api;

import com.wps.yundoc.capability.userfile.application.UserFileListCommand;
import com.wps.yundoc.capability.userfile.application.UserFileListResult;
import com.wps.yundoc.capability.userfile.application.UserFileService;
import com.wps.yundoc.auth.application.UserAssertionVerifier;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * UserFileController component.
 *
 * @author WPS
 */
@RestController
@RequestMapping("/api/v1/user/files")
public class UserFileController {

    private static final int MAX_LIMIT = 200;
    private static final int MAX_USER_ID_LENGTH = 128;
    private static final int MAX_PARENT_FILE_ID_LENGTH = 128;
    private static final int MAX_CURSOR_LENGTH = 512;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:@-]+$");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^[A-Za-z0-9._:@/+=-]+$");

    private final UserFileService userFileService;
    private final UserAssertionVerifier userAssertionVerifier;

    public UserFileController(UserFileService userFileService, UserAssertionVerifier userAssertionVerifier) {
        this.userFileService = userFileService;
        this.userAssertionVerifier = userAssertionVerifier;
    }

    @GetMapping
    public ApiResponse<UserFileListResponse> listFiles(
            HttpServletRequest request,
            @RequestParam(value = "userId", required = false) List<String> queryUserIds,
            @RequestParam(value = "parentFileId", required = false) String parentFileId,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "cursor", required = false) String cursor) {
        UserFileListCommand command = command(queryUserIds, parentFileId, limit, cursor);
        userAssertionVerifier.verify(request, command.getUserId());
        UserFileListResult result = userFileService.listFiles(command);
        return ApiResponse.success(new UserFileListResponse(result), requestId());
    }

    private UserFileListCommand command(
            List<String> queryUserIds,
            String parentFileId,
            int limit,
            String cursor) {
        String normalizedParentFileId = normalized(parentFileId);
        String normalizedCursor = normalized(cursor);
        validateResource(normalizedParentFileId, MAX_PARENT_FILE_ID_LENGTH);
        validateResource(normalizedCursor, MAX_CURSOR_LENGTH);
        return new UserFileListCommand(
                queryUserId(queryUserIds),
                businessSystemId(),
                normalizedParentFileId,
                validatedLimit(limit),
                normalizedCursor);
    }

    private String queryUserId(List<String> queryUserIds) {
        if (queryUserIds == null || queryUserIds.isEmpty()) {
            return null;
        }
        String first = requiredUserId(queryUserIds.get(0));
        validateUserId(first);
        for (String userId : queryUserIds) {
            validateSameUserId(first, userId);
        }
        return first;
    }

    private void validateSameUserId(String first, String current) {
        String currentUserId = requiredUserId(current);
        if (Objects.equals(first, currentUserId)) {
            return;
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private void validateUserId(String userId) {
        if (userId == null) {
            return;
        }
        validateLength(userId, MAX_USER_ID_LENGTH);
        validatePattern(userId, USER_ID_PATTERN);
    }

    private void validateResource(String value, int maxLength) {
        if (value == null) {
            return;
        }
        validateLength(value, maxLength);
        validatePattern(value, RESOURCE_PATTERN);
    }

    private void validateLength(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return;
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private void validatePattern(String value, Pattern pattern) {
        if (pattern.matcher(value).matches()) {
            return;
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private int validatedLimit(int limit) {
        if (limitInRange(limit)) {
            return limit;
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private boolean limitInRange(int limit) {
        return limit > 0 && limit <= MAX_LIMIT;
    }

    private String businessSystemId() {
        return requestContext().getBusinessSystemId();
    }

    private String requestId() {
        return requestContext().getRequestId();
    }

    private RequestContext requestContext() {
        return RequestContextHolder.current()
                .orElseThrow(() -> new YundocException(YundocErrorCode.TOKEN_INVALID));
    }

    private String requiredUserId(String value) {
        if (Texts.hasText(value)) {
            return value.trim();
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private String normalized(String value) {
        if (!Texts.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
