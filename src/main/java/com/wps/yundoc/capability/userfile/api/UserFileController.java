package com.wps.yundoc.capability.userfile.api;

import com.wps.yundoc.capability.userfile.application.UserFileDownloadCommand;
import com.wps.yundoc.capability.userfile.application.UserFileDownloadResult;
import com.wps.yundoc.capability.userfile.application.UserFileListCommand;
import com.wps.yundoc.capability.userfile.application.UserFileListResult;
import com.wps.yundoc.capability.userfile.application.UserFileSearchCommand;
import com.wps.yundoc.capability.userfile.application.UserFileSearchResult;
import com.wps.yundoc.capability.userfile.application.UserFileService;
import com.wps.yundoc.capability.userfile.application.UserFileUploadCommand;
import com.wps.yundoc.capability.userfile.application.UserFileUploadResult;
import com.wps.yundoc.common.api.ApiResponse;
import com.wps.yundoc.common.context.RequestContext;
import com.wps.yundoc.common.context.RequestContextHolder;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * UserFileController 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
@RestController
@RequestMapping("/api/v1/user/files")
public class UserFileController {

    private static final String PARAM_USER_ID = "userId";
    private static final String DEFAULT_LIMIT_VALUE = "50";
    private static final String CURRENT_DIRECTORY = ".";
    private static final String PARENT_DIRECTORY = "..";
    private static final int MAX_LIMIT = 200;
    private static final int MAX_USER_ID_LENGTH = 128;
    private static final int MAX_KEYWORD_LENGTH = 128;
    private static final int MAX_PARENT_FILE_ID_LENGTH = 128;
    private static final int MAX_CURSOR_LENGTH = 512;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:@-]+$");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^[A-Za-z0-9._:@/+=-]+$");
    private static final Pattern PATH_RESOURCE_PATTERN = Pattern.compile("^[A-Za-z0-9._:@+=-]+$");

    private final UserFileService userFileService;

    public UserFileController(UserFileService userFileService) {
        this.userFileService = userFileService;
    }

    @GetMapping
    public ApiResponse<UserFileListResponse> listFiles(
            @RequestParam(value = PARAM_USER_ID, required = false) List<String> queryUserIds,
            @RequestParam(value = "parentFileId", required = false) String parentFileId,
            @RequestParam(value = "limit", required = false, defaultValue = DEFAULT_LIMIT_VALUE) int limit,
            @RequestParam(value = "cursor", required = false) String cursor) {
        UserFileListCommand command = command(queryUserIds, parentFileId, limit, cursor);
        UserFileListResult result = userFileService.listFiles(command);
        return ApiResponse.success(new UserFileListResponse(result), requestId());
    }

    @GetMapping("/search")
    public ApiResponse<UserFileSearchResponse> searchFiles(
            @RequestParam(value = PARAM_USER_ID, required = false) List<String> queryUserIds,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", required = false, defaultValue = DEFAULT_LIMIT_VALUE) int limit,
            @RequestParam(value = "cursor", required = false) String cursor) {
        UserFileSearchResult result = userFileService.searchFiles(searchCommand(
                queryUserIds,
                keyword,
                limit,
                cursor));
        return ApiResponse.success(new UserFileSearchResponse(result), requestId());
    }

    @PostMapping("/{fileId}/download-url")
    public ApiResponse<UserFileDownloadResponse> downloadInfo(
            @PathVariable("fileId") String fileId,
            @RequestParam(value = PARAM_USER_ID, required = false) List<String> queryUserIds,
            @RequestParam("driveId") String driveId) {
        UserFileDownloadResult result = userFileService.downloadInfo(downloadCommand(
                queryUserIds,
                driveId,
                fileId));
        return ApiResponse.success(new UserFileDownloadResponse(result), requestId());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserFileUploadResponse> uploadFile(
            @RequestParam(value = PARAM_USER_ID, required = false) List<String> queryUserIds,
            @RequestParam("driveId") String driveId,
            @RequestParam("parentFileId") String parentFileId,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam("file") MultipartFile file) {
        UserFileUploadResult result = userFileService.uploadFile(uploadCommand(
                queryUserIds,
                driveId,
                parentFileId,
                displayName,
                file));
        return ApiResponse.success(new UserFileUploadResponse(result), requestId());
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
        String contextUserId = contextUserId();
        validateQueryUserId(queryUserIds, contextUserId);
        return UserFileListCommand.builder()
                .userId(contextUserId)
                .businessSystemId(businessSystemId())
                .clientId(clientId())
                .parentFileId(normalizedParentFileId)
                .limit(validatedLimit(limit))
                .cursor(normalizedCursor)
                .build();
    }

    private UserFileSearchCommand searchCommand(
            List<String> queryUserIds,
            String keyword,
            int limit,
            String cursor) {
        String normalizedKeyword = requiredKeyword(keyword);
        String normalizedCursor = normalized(cursor);
        validateLength(normalizedKeyword, MAX_KEYWORD_LENGTH);
        validateResource(normalizedCursor, MAX_CURSOR_LENGTH);
        String contextUserId = contextUserId();
        validateQueryUserId(queryUserIds, contextUserId);
        return UserFileSearchCommand.builder()
                .userId(contextUserId)
                .businessSystemId(businessSystemId())
                .clientId(clientId())
                .keyword(normalizedKeyword)
                .limit(validatedLimit(limit))
                .cursor(normalizedCursor)
                .build();
    }

    private UserFileDownloadCommand downloadCommand(
            List<String> queryUserIds,
            String driveId,
            String fileId) {
        String normalizedDriveId = requiredResource(driveId);
        String normalizedFileId = requiredResource(fileId);
        validatePathResource(normalizedDriveId, MAX_PARENT_FILE_ID_LENGTH);
        validatePathResource(normalizedFileId, MAX_PARENT_FILE_ID_LENGTH);
        String contextUserId = contextUserId();
        validateQueryUserId(queryUserIds, contextUserId);
        return new UserFileDownloadCommand(
                contextUserId,
                businessSystemId(),
                clientId(),
                normalizedDriveId,
                normalizedFileId);
    }

    private UserFileUploadCommand uploadCommand(
            List<String> queryUserIds,
            String driveId,
            String parentFileId,
            String displayName,
            MultipartFile file) {
        String normalizedDriveId = pathResource(driveId);
        String normalizedParentFileId = pathResource(parentFileId);
        String contextUserId = contextUserId();
        validateQueryUserId(queryUserIds, contextUserId);
        return UserFileUploadCommand.builder()
                .userId(contextUserId)
                .businessSystemId(businessSystemId())
                .clientId(clientId())
                .driveId(normalizedDriveId)
                .parentFileId(normalizedParentFileId)
                .file(file)
                .displayName(normalized(displayName))
                .build();
    }

    private String pathResource(String value) {
        String normalizedValue = requiredResource(value);
        validatePathResource(normalizedValue, MAX_PARENT_FILE_ID_LENGTH);
        return normalizedValue;
    }

    private void validateQueryUserId(List<String> queryUserIds, String contextUserId) {
        if (queryUserIds == null || queryUserIds.isEmpty()) {
            return;
        }
        String first = requiredUserId(queryUserIds.get(0));
        validateUserId(first);
        validateSameUserId(contextUserId, first);
        for (String userId : queryUserIds) {
            validateSameUserId(first, userId);
        }
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

    private void validatePathResource(String value, int maxLength) {
        if (value == null) {
            return;
        }
        validateLength(value, maxLength);
        validatePattern(value, PATH_RESOURCE_PATTERN);
        validatePathTraversalToken(value);
    }

    private void validatePathTraversalToken(String value) {
        if (CURRENT_DIRECTORY.equals(value) || value.contains(PARENT_DIRECTORY)) {
            throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
        }
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

    private String clientId() {
        return requestContext().getClientId();
    }

    private String contextUserId() {
        String userId = requestContext().getUserId();
        if (Texts.hasText(userId)) {
            validateUserId(userId);
            return userId;
        }
        throw new YundocException(YundocErrorCode.USER_ID_REQUIRED);
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

    private String requiredKeyword(String value) {
        if (Texts.hasText(value)) {
            return value.trim();
        }
        throw new YundocException(YundocErrorCode.VALIDATION_FAILED);
    }

    private String requiredResource(String value) {
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
