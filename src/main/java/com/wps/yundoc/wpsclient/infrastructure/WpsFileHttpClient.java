package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.wpsclient.application.WpsCommitUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateDriveRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateFolderRequest;
import com.wps.yundoc.wpsclient.application.WpsDrive;
import com.wps.yundoc.wpsclient.application.WpsDriveList;
import com.wps.yundoc.wpsclient.application.WpsDriveListRequest;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileChildrenRequest;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import com.wps.yundoc.wpsclient.application.WpsRequestUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsStoreRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * WpsFileHttpClient component.
 *
 * @author WPS
 */
public class WpsFileHttpClient implements WpsFileClient {

    private static final HttpMethod UPLOAD_METHOD = HttpMethod.PUT;

    private final WpsClientProperties properties;
    private final RestTemplate restTemplate;
    private final WpsRequestSigner signer;

    public WpsFileHttpClient(WpsClientProperties properties, RestTemplateBuilder builder) {
        this(properties, builder, WpsClientSupport.restTemplate(properties, builder));
    }

    public WpsFileHttpClient(
            WpsClientProperties properties,
            RestTemplateBuilder builder,
            RestTemplate restTemplate) {
        Objects.requireNonNull(builder, "builder");
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.signer = WpsRequestSigner.fromProperties(properties);
    }

    @Override
    public WpsFileList listFiles(WpsFileListRequest request) {
        WpsFileListResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(request));
        return toFileList(response);
    }

    @Override
    public WpsDriveList listDrives(WpsDriveListRequest request) {
        WpsDriveListResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(request));
        return toDriveList(response);
    }

    @Override
    public WpsDrive createDrive(WpsCreateDriveRequest request) {
        WpsDriveResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(request));
        return toDrive(response);
    }

    @Override
    public WpsFileList listChildren(WpsFileChildrenRequest request) {
        WpsFileListResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(request));
        return toFileList(response);
    }

    @Override
    public WpsFileItem createFolder(WpsCreateFolderRequest request) {
        WpsFileItemResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(request));
        return toFileItem(response);
    }

    @Override
    public WpsUploadInfo requestUpload(WpsRequestUploadRequest request) {
        WpsRequestUploadResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(request));
        return toUploadInfo(response);
    }

    @Override
    public void uploadFile(WpsUploadFileRequest request) {
        WpsClientSupport.executeWithRetry(properties, () -> {
            exchange(request);
            return null;
        });
    }

    @Override
    public WpsFileItem commitUpload(WpsCommitUploadRequest request) {
        WpsFileItemResponse response = WpsClientSupport.executeWithRetry(
                properties,
                () -> exchange(request));
        return toFileItem(response);
    }

    private WpsFileListResponse exchange(WpsFileListRequest request) {
        String url = fileListUrl(request);
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity(request, url),
                WpsFileListResponse.class).getBody();
    }

    private WpsDriveListResponse exchange(WpsDriveListRequest request) {
        String url = driveListUrl(request);
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity(request.getAccessToken(), url),
                WpsDriveListResponse.class).getBody();
    }

    private WpsDriveResponse exchange(WpsCreateDriveRequest request) {
        String url = driveCreateUrl();
        CreateDrivePayload payload = new CreateDrivePayload(
                request.getName(),
                request.getSource(),
                request.getTotalQuota());
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                jsonEntity(request.getAccessToken(), url, HttpMethod.POST, payload),
                WpsDriveResponse.class).getBody();
    }

    private WpsFileListResponse exchange(WpsFileChildrenRequest request) {
        String url = fileChildrenUrl(request);
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity(request.getAccessToken(), url),
                WpsFileListResponse.class).getBody();
    }

    private WpsFileItemResponse exchange(WpsCreateFolderRequest request) {
        String url = fileCreateUrl(request.getDriveId(), request.getParentFileId());
        CreateFolderPayload payload = new CreateFolderPayload(request.getName(), request.getOnNameConflict());
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                jsonEntity(request.getAccessToken(), url, HttpMethod.POST, payload),
                WpsFileItemResponse.class).getBody();
    }

    private WpsRequestUploadResponse exchange(WpsRequestUploadRequest request) {
        String url = requestUploadUrl(request.getDriveId(), request.getParentFileId());
        RequestUploadPayload payload = new RequestUploadPayload(
                request.getHashes(),
                request.isInternal(),
                request.getName(),
                request.getOnNameConflict(),
                request.getSize());
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                jsonEntity(request.getAccessToken(), url, HttpMethod.POST, payload),
                WpsRequestUploadResponse.class).getBody();
    }

    private void exchange(WpsUploadFileRequest request) {
        String url = request.getStoreRequest().getUrl();
        validateUploadUrl(url);
        restTemplate.exchange(
                url,
                uploadMethod(request.getStoreRequest()),
                uploadEntity(request),
                Void.class);
    }

    private WpsFileItemResponse exchange(WpsCommitUploadRequest request) {
        String url = commitUploadUrl(request.getDriveId(), request.getParentFileId());
        CommitUploadPayload payload = new CommitUploadPayload(request.getUploadId());
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                jsonEntity(request.getAccessToken(), url, HttpMethod.POST, payload),
                WpsFileItemResponse.class).getBody();
    }

    private WpsFileList toFileList(WpsFileListResponse response) {
        FileListData data = requireData(response);
        return new WpsFileList(toItems(data.getItems()), data.getNextCursor());
    }

    private WpsDriveList toDriveList(WpsDriveListResponse response) {
        DriveListData data = WpsClientSupport.requireSuccessData(response);
        return new WpsDriveList(toDrives(data.getItems()), data.getNextPageToken());
    }

    private WpsDrive toDrive(WpsDriveResponse response) {
        return toDrive(WpsClientSupport.requireSuccessData(response));
    }

    private WpsFileItem toFileItem(WpsFileItemResponse response) {
        return toItem(WpsClientSupport.requireSuccessData(response));
    }

    private WpsUploadInfo toUploadInfo(WpsRequestUploadResponse response) {
        UploadInfoData data = WpsClientSupport.requireSuccessData(response);
        StoreRequestData storeRequest = WpsClientSupport.requireData(data.getStoreRequest());
        return new WpsUploadInfo(
                WpsClientSupport.requireText(data.getUploadId()),
                new WpsStoreRequest(
                        WpsClientSupport.requireText(storeRequest.getMethod()),
                        WpsClientSupport.requireText(storeRequest.getUrl())));
    }

    private FileListData requireData(WpsFileListResponse response) {
        return WpsClientSupport.requireSuccessData(response);
    }

    private List<WpsFileItem> toItems(List<FileListItemData> items) {
        List<WpsFileItem> result = new ArrayList<>();
        for (FileListItemData item : safeItems(items)) {
            result.add(toItem(item));
        }
        return result;
    }

    private List<WpsDrive> toDrives(List<DriveData> items) {
        List<WpsDrive> result = new ArrayList<>();
        for (DriveData item : safeDrives(items)) {
            result.add(toDrive(item));
        }
        return result;
    }

    private List<FileListItemData> safeItems(List<FileListItemData> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    private List<DriveData> safeDrives(List<DriveData> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    private WpsDrive toDrive(DriveData item) {
        if (item == null) {
            throw WpsClientSupport.upstreamError(null);
        }
        return new WpsDrive(
                WpsClientSupport.requireText(item.getDriveId()),
                item.getName(),
                item.getStatus(),
                item.getSource());
    }

    private WpsFileItem toItem(FileListItemData item) {
        if (item == null) {
            throw WpsClientSupport.upstreamError(null);
        }
        return new WpsFileItem(
                item.getFileId(),
                item.getName(),
                item.getType(),
                item.isFolder(),
                item.getUpdatedAt());
    }

    private HttpEntity<Void> entity(WpsFileListRequest request, String url) {
        return entity(request.getAccessToken(), url);
    }

    private HttpEntity<Void> entity(String accessToken, String url) {
        HttpHeaders headers = WpsSignedRequestSupport.signedJsonHeaders(
                properties,
                signer,
                HttpMethod.GET.name(),
                url,
                new byte[0]);
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<byte[]> jsonEntity(
            String accessToken,
            String url,
            HttpMethod method,
            Object payload) {
        byte[] body = WpsSignedRequestSupport.jsonBody(payload);
        HttpHeaders headers = WpsSignedRequestSupport.signedJsonHeaders(
                properties,
                signer,
                method.name(),
                url,
                body);
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<FileSystemResource> uploadEntity(WpsUploadFileRequest request) {
        HttpHeaders headers = WpsSignedRequestSupport.signedOctetStreamHeaders(
                properties,
                signer,
                request,
                uploadMethod(request.getStoreRequest()));
        headers.setBearerAuth(request.getAccessToken());
        return new HttpEntity<>(new FileSystemResource(request.getFile().toFile()), headers);
    }

    private String fileListUrl(WpsFileListRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseFileListUrl())
                .queryParam("parentFileId", request.getParentFileId())
                .queryParam("limit", Integer.valueOf(request.getLimit()));
        addCursor(builder, request.getCursor());
        return builder.toUriString();
    }

    private String driveListUrl(WpsDriveListRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl()
                        + properties.getDriveListPath())
                .queryParam("allotee_type", "app")
                .queryParam("page_size", Integer.valueOf(request.getPageSize()));
        addPageToken(builder, request.getPageToken());
        return builder.toUriString();
    }

    private String fileChildrenUrl(WpsFileChildrenRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(fileChildrenBaseUrl(request))
                .queryParam("filter_type", "folder")
                .queryParam("page_size", Integer.valueOf(request.getPageSize()));
        addPageToken(builder, request.getPageToken());
        return builder.toUriString();
    }

    private void addCursor(UriComponentsBuilder builder, String cursor) {
        if (Texts.hasText(cursor)) {
            builder.queryParam("cursor", cursor);
        }
    }

    private void addPageToken(UriComponentsBuilder builder, String pageToken) {
        if (Texts.hasText(pageToken)) {
            builder.queryParam("page_token", pageToken);
        }
    }

    private String baseFileListUrl() {
        return properties.getBaseUrl() + properties.getFileListPath();
    }

    private String driveCreateUrl() {
        return properties.getBaseUrl() + properties.getDriveCreatePath();
    }

    private String fileChildrenBaseUrl(WpsFileChildrenRequest request) {
        return properties.getBaseUrl() + path(
                properties.getFileChildrenPathTemplate(),
                request.getDriveId(),
                request.getParentFileId());
    }

    private String fileCreateUrl(String driveId, String parentFileId) {
        return properties.getBaseUrl() + path(properties.getFileCreatePathTemplate(), driveId, parentFileId);
    }

    private String requestUploadUrl(String driveId, String parentFileId) {
        return properties.getBaseUrl() + path(properties.getRequestUploadPathTemplate(), driveId, parentFileId);
    }

    private String commitUploadUrl(String driveId, String parentFileId) {
        return properties.getBaseUrl() + path(properties.getCommitUploadPathTemplate(), driveId, parentFileId);
    }

    private String path(String template, String driveId, String parentFileId) {
        return template
                .replace("{drive_id}", driveId)
                .replace("{driveId}", driveId)
                .replace("{parent_id}", parentFileId)
                .replace("{parentId}", parentFileId);
    }

    private HttpMethod uploadMethod(WpsStoreRequest storeRequest) {
        if (!UPLOAD_METHOD.name().equalsIgnoreCase(storeRequest.getMethod())) {
            throw WpsClientSupport.upstreamError(null);
        }
        return UPLOAD_METHOD;
    }

    private void validateUploadUrl(String url) {
        URI uri = uploadUri(url);
        if (!isAllowedUploadUri(uri)) {
            throw WpsClientSupport.upstreamError(null);
        }
    }

    private URI uploadUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException ex) {
            throw WpsClientSupport.upstreamError(ex);
        }
    }

    private boolean isAllowedUploadUri(URI uri) {
        return WpsClientSupport.isSecureHttpsUri(uri)
                && uri.getUserInfo() == null
                && uri.getFragment() == null
                && isAllowedUploadHost(uri.getHost());
    }

    private boolean isAllowedUploadHost(String host) {
        if (!Texts.hasText(host)) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        for (String suffix : uploadHostSuffixes()) {
            if (matchesSuffix(normalized, suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSuffix(String host, String suffix) {
        String normalizedSuffix = suffix.toLowerCase(Locale.ROOT);
        return host.equals(normalizedSuffix) || host.endsWith("." + normalizedSuffix);
    }

    private List<String> uploadHostSuffixes() {
        List<String> suffixes = properties.getUploadUrlAllowedHostSuffixes();
        if (suffixes == null || suffixes.isEmpty()) {
            return Collections.singletonList(uploadUri(properties.getBaseUrl()).getHost());
        }
        return suffixes;
    }
}
