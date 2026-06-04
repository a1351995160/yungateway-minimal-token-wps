package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.wpsclient.application.WpsCommitUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateDriveRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateFolderRequest;
import com.wps.yundoc.wpsclient.application.WpsDrive;
import com.wps.yundoc.wpsclient.application.WpsDriveList;
import com.wps.yundoc.wpsclient.application.WpsDriveListRequest;
import com.wps.yundoc.wpsclient.application.WpsDownloadHash;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileChildrenRequest;
import com.wps.yundoc.wpsclient.application.WpsFileDownloadInfo;
import com.wps.yundoc.wpsclient.application.WpsFileDownloadRequest;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import com.wps.yundoc.wpsclient.application.WpsFileSearchRequest;
import com.wps.yundoc.wpsclient.application.WpsRequestUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsStoreRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * WpsFileHttpClient 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class WpsFileHttpClient implements WpsFileClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WpsFileHttpClient.class);

    private static final HttpMethod UPLOAD_METHOD = HttpMethod.PUT;
    private static final int HASH_PREFIX_LENGTH = 12;
    private static final int PATH_VARIABLE_COUNT = 6;
    private static final String PAGE_SIZE_PARAM = "page_size";
    private static final String PAGE_TOKEN_PARAM = "page_token";

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
        LOGGER.info("WPS请求开始 操作=查询文件列表 父文件ID={} 分页大小={}",
                request.getParentFileId(),
                Integer.valueOf(request.getLimit()));
        WpsFileListResponse response = executeWpsOperation("查询文件列表", () -> exchange(request));
        WpsFileList fileList = toFileList(response);
        LOGGER.info("WPS请求结果 操作=查询文件列表 文件数量={} 是否有下一页={}",
                Integer.valueOf(fileList.getItems().size()),
                Boolean.valueOf(Texts.hasText(fileList.getNextCursor())));
        return fileList;
    }

    @Override
    public WpsFileList searchFiles(WpsFileSearchRequest request) {
        LOGGER.info("WPS请求开始 操作=搜索文件 关键词长度={} 分页大小={} 是否有分页标识={}",
                Integer.valueOf(request.getKeyword().length()),
                Integer.valueOf(request.getPageSize()),
                Boolean.valueOf(Texts.hasText(request.getPageToken())));
        WpsFileSearchResponse response = executeWpsOperation("搜索文件", () -> exchange(request));
        WpsFileList fileList = toSearchFileList(response);
        LOGGER.info("WPS请求结果 操作=搜索文件 文件数量={} 是否有下一页={}",
                Integer.valueOf(fileList.getItems().size()),
                Boolean.valueOf(Texts.hasText(fileList.getNextCursor())));
        return fileList;
    }

    @Override
    public WpsFileDownloadInfo downloadInfo(WpsFileDownloadRequest request) {
        LOGGER.info("WPS请求开始 操作=获取下载信息 空间ID={} 文件ID={} 是否返回哈希={} 是否内网地址={}",
                request.getDriveId(),
                request.getFileId(),
                Boolean.valueOf(request.isWithHash()),
                Boolean.valueOf(request.isInternal()));
        WpsFileDownloadResponse response = executeWpsOperation("获取下载信息", () -> exchange(request));
        WpsFileDownloadInfo downloadInfo = toDownloadInfo(response);
        LOGGER.info("WPS请求结果 操作=获取下载信息 空间ID={} 文件ID={} 哈希数量={}",
                request.getDriveId(),
                request.getFileId(),
                Integer.valueOf(downloadInfo.getHashes().size()));
        return downloadInfo;
    }

    @Override
    public WpsDriveList listDrives(WpsDriveListRequest request) {
        LOGGER.info("WPS请求开始 操作=查询空间列表 分页大小={} 是否有分页标识={}",
                Integer.valueOf(request.getPageSize()),
                Boolean.valueOf(Texts.hasText(request.getPageToken())));
        WpsDriveListResponse response = executeWpsOperation("查询空间列表", () -> exchange(request));
        WpsDriveList driveList = toDriveList(response);
        LOGGER.info("WPS请求结果 操作=查询空间列表 空间数量={} 是否有下一页={}",
                Integer.valueOf(driveList.getItems().size()),
                Boolean.valueOf(Texts.hasText(driveList.getNextPageToken())));
        return driveList;
    }

    @Override
    public WpsDrive createDrive(WpsCreateDriveRequest request) {
        LOGGER.info("WPS请求开始 操作=创建空间 空间名称={} 来源={}",
                request.getName(),
                request.getSource());
        WpsDriveResponse response = executeWpsOperation("创建空间", () -> exchange(request));
        WpsDrive drive = toDrive(response);
        LOGGER.info("WPS请求结果 操作=创建空间 空间ID={} 空间名称={}",
                drive.getDriveId(),
                drive.getName());
        return drive;
    }

    @Override
    public WpsFileList listChildren(WpsFileChildrenRequest request) {
        LOGGER.info("WPS请求开始 操作=查询子文件 空间ID={} 父文件ID={} 分页大小={}",
                request.getDriveId(),
                request.getParentFileId(),
                Integer.valueOf(request.getPageSize()));
        WpsFileListResponse response = executeWpsOperation("查询子文件", () -> exchange(request));
        WpsFileList fileList = toFileList(response);
        LOGGER.info("WPS请求结果 操作=查询子文件 空间ID={} 父文件ID={} 文件数量={} 是否有下一页={}",
                request.getDriveId(),
                request.getParentFileId(),
                Integer.valueOf(fileList.getItems().size()),
                Boolean.valueOf(Texts.hasText(fileList.getNextCursor())));
        return fileList;
    }

    @Override
    public WpsFileItem createFolder(WpsCreateFolderRequest request) {
        LOGGER.info("WPS请求开始 操作=创建文件夹 空间ID={} 父文件ID={} 文件夹名称={}",
                request.getDriveId(),
                request.getParentFileId(),
                request.getName());
        WpsFileItemResponse response = executeWpsOperation("创建文件夹", () -> exchange(request));
        WpsFileItem folder = toFileItem(response);
        LOGGER.info("WPS请求结果 操作=创建文件夹 空间ID={} 父文件ID={} 文件夹ID={}",
                request.getDriveId(),
                request.getParentFileId(),
                folder.getFileId());
        return folder;
    }

    @Override
    public WpsUploadInfo requestUpload(WpsRequestUploadRequest request) {
        LOGGER.info("WPS请求开始 操作=申请上传信息 空间ID={} 父文件ID={} 文件名={} 文件大小={}",
                request.getDriveId(),
                request.getParentFileId(),
                request.getName(),
                Long.valueOf(request.getSize()));
        WpsRequestUploadResponse response = executeWpsOperation("申请上传信息", () -> exchange(request));
        WpsUploadInfo uploadInfo = toUploadInfo(response);
        LOGGER.info("WPS请求结果 操作=申请上传信息 空间ID={} 父文件ID={} 上传ID={}",
                request.getDriveId(),
                request.getParentFileId(),
                uploadInfo.getUploadId());
        return uploadInfo;
    }

    @Override
    public void uploadFile(WpsUploadFileRequest request) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("WPS请求开始 操作=上传实体文件 文件大小={} 文件摘要前缀={} 请求方法={}",
                    Long.valueOf(request.getSize()),
                    sha256Prefix(request.getSha256()),
                    request.getStoreRequest().getMethod());
        }
        executeWpsOperation("上传实体文件", () -> {
            exchange(request);
            return null;
        });
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("WPS请求结果 操作=上传实体文件 文件大小={} 文件摘要前缀={}",
                    Long.valueOf(request.getSize()),
                    sha256Prefix(request.getSha256()));
        }
    }

    @Override
    public WpsFileItem commitUpload(WpsCommitUploadRequest request) {
        LOGGER.info("WPS请求开始 操作=提交上传完成 空间ID={} 父文件ID={} 上传ID={}",
                request.getDriveId(),
                request.getParentFileId(),
                request.getUploadId());
        WpsFileItemResponse response = executeWpsOperation("提交上传完成", () -> exchange(request));
        WpsFileItem fileItem = toFileItem(response);
        LOGGER.info("WPS请求结果 操作=提交上传完成 空间ID={} 父文件ID={} 上传ID={} WPS文件ID={}",
                request.getDriveId(),
                request.getParentFileId(),
                request.getUploadId(),
                fileItem.getFileId());
        return fileItem;
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

    private WpsFileSearchResponse exchange(WpsFileSearchRequest request) {
        String url = fileSearchUrl(request);
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity(request.getAccessToken(), url),
                WpsFileSearchResponse.class).getBody();
    }

    private WpsFileDownloadResponse exchange(WpsFileDownloadRequest request) {
        URI uri = fileDownloadUri(request);
        String url = uri.toString();
        return restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity(request.getAccessToken(), url),
                WpsFileDownloadResponse.class).getBody();
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
        URI uri = fileChildrenUri(request);
        String url = uri.toString();
        return restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity(request.getAccessToken(), url),
                WpsFileListResponse.class).getBody();
    }

    private WpsFileItemResponse exchange(WpsCreateFolderRequest request) {
        URI uri = fileCreateUri(request.getDriveId(), request.getParentFileId());
        String url = uri.toString();
        CreateFolderPayload payload = new CreateFolderPayload(request.getName(), request.getOnNameConflict());
        return restTemplate.exchange(
                uri,
                HttpMethod.POST,
                jsonEntity(request.getAccessToken(), url, HttpMethod.POST, payload),
                WpsFileItemResponse.class).getBody();
    }

    private WpsRequestUploadResponse exchange(WpsRequestUploadRequest request) {
        URI uri = requestUploadUri(request.getDriveId(), request.getParentFileId());
        String url = uri.toString();
        RequestUploadPayload payload = new RequestUploadPayload(
                request.getHashes(),
                request.isInternal(),
                request.getName(),
                request.getOnNameConflict(),
                request.getSize());
        return restTemplate.exchange(
                uri,
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
        URI uri = commitUploadUri(request.getDriveId(), request.getParentFileId());
        String url = uri.toString();
        CommitUploadPayload payload = new CommitUploadPayload(request.getUploadId());
        return restTemplate.exchange(
                uri,
                HttpMethod.POST,
                jsonEntity(request.getAccessToken(), url, HttpMethod.POST, payload),
                WpsFileItemResponse.class).getBody();
    }

    private <T> T executeWpsOperation(String operation, WpsClientSupport.WpsCall<T> call) {
        long startedAt = System.nanoTime();
        T result = WpsClientSupport.executeWithRetry(properties, operation, call);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("WPS请求完成 操作={} 耗时毫秒={}",
                    operation,
                    Long.valueOf(elapsedMillis(startedAt)));
        }
        return result;
    }

    private WpsFileList toFileList(WpsFileListResponse response) {
        FileListData data = requireData(response);
        return new WpsFileList(toItems(data.getItems()), data.getNextCursor());
    }

    private WpsDriveList toDriveList(WpsDriveListResponse response) {
        DriveListData data = WpsClientSupport.requireSuccessData(response);
        return new WpsDriveList(toDrives(data.getItems()), data.getNextPageToken());
    }

    private WpsFileList toSearchFileList(WpsFileSearchResponse response) {
        SearchFileListData data = WpsClientSupport.requireSuccessData(response);
        return new WpsFileList(toSearchItems(data.getItems()), data.getNextCursor());
    }

    private WpsFileDownloadInfo toDownloadInfo(WpsFileDownloadResponse response) {
        DownloadInfoData data = WpsClientSupport.requireSuccessData(response);
        return new WpsFileDownloadInfo(
                WpsClientSupport.requireText(data.getUrl()),
                toDownloadHashes(data.getHashes()));
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

    private List<WpsFileItem> toSearchItems(List<SearchFileItemData> items) {
        List<WpsFileItem> result = new ArrayList<>();
        for (SearchFileItemData item : safeSearchItems(items)) {
            result.add(toSearchItem(item));
        }
        return result;
    }

    private WpsFileItem toSearchItem(SearchFileItemData item) {
        if (item == null) {
            throw WpsClientSupport.upstreamError(null);
        }
        return toItem(WpsClientSupport.requireData(item.getFile()));
    }

    private List<WpsDownloadHash> toDownloadHashes(List<DownloadHashData> hashes) {
        List<WpsDownloadHash> result = new ArrayList<>();
        for (DownloadHashData hash : safeHashes(hashes)) {
            if (hash == null) {
                throw WpsClientSupport.upstreamError(null);
            }
            result.add(new WpsDownloadHash(
                    WpsClientSupport.requireText(hash.getType()),
                    WpsClientSupport.requireText(hash.getSum())));
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

    private List<SearchFileItemData> safeSearchItems(List<SearchFileItemData> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    private List<DownloadHashData> safeHashes(List<DownloadHashData> hashes) {
        if (hashes == null) {
            return Collections.emptyList();
        }
        return hashes;
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
        return WpsFileItem.builder()
                .fileId(item.getFileId())
                .driveId(item.getDriveId())
                .name(item.getName())
                .type(item.getType())
                .folder(item.isFolder())
                .updatedAt(item.getUpdatedAt())
                .build();
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
                .queryParam(PAGE_SIZE_PARAM, Integer.valueOf(request.getPageSize()));
        addPageToken(builder, request.getPageToken());
        return builder.toUriString();
    }

    private String fileSearchUrl(WpsFileSearchRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl()
                        + properties.getFileSearchPath())
                .queryParam("keyword", request.getKeyword())
                .queryParam("type", "all")
                .queryParam(PAGE_SIZE_PARAM, Integer.valueOf(request.getPageSize()))
                .queryParam("with_drive", Boolean.TRUE);
        addPageToken(builder, request.getPageToken());
        return builder.toUriString();
    }

    private URI fileDownloadUri(WpsFileDownloadRequest request) {
        return pathBuilder(properties.getFileDownloadPathTemplate())
                .queryParam("with_hash", Boolean.valueOf(request.isWithHash()))
                .queryParam("internal", Boolean.valueOf(request.isInternal()))
                .buildAndExpand(pathVariables(request.getDriveId(), null, request.getFileId()))
                .toUri();
    }

    private URI fileChildrenUri(WpsFileChildrenRequest request) {
        UriComponentsBuilder builder = pathBuilder(properties.getFileChildrenPathTemplate())
                .queryParam("filter_type", "folder")
                .queryParam(PAGE_SIZE_PARAM, Integer.valueOf(request.getPageSize()));
        addPageToken(builder, request.getPageToken());
        return builder.buildAndExpand(pathVariables(request.getDriveId(), request.getParentFileId(), null))
                .toUri();
    }

    private void addCursor(UriComponentsBuilder builder, String cursor) {
        if (Texts.hasText(cursor)) {
            builder.queryParam("cursor", cursor);
        }
    }

    private void addPageToken(UriComponentsBuilder builder, String pageToken) {
        if (Texts.hasText(pageToken)) {
            builder.queryParam(PAGE_TOKEN_PARAM, pageToken);
        }
    }

    private String baseFileListUrl() {
        return properties.getBaseUrl() + properties.getFileListPath();
    }

    private String driveCreateUrl() {
        return properties.getBaseUrl() + properties.getDriveCreatePath();
    }

    private URI fileCreateUri(String driveId, String parentFileId) {
        return expandedPathUri(properties.getFileCreatePathTemplate(), driveId, parentFileId, null);
    }

    private URI requestUploadUri(String driveId, String parentFileId) {
        return expandedPathUri(properties.getRequestUploadPathTemplate(), driveId, parentFileId, null);
    }

    private URI commitUploadUri(String driveId, String parentFileId) {
        return expandedPathUri(properties.getCommitUploadPathTemplate(), driveId, parentFileId, null);
    }

    private URI expandedPathUri(String template, String driveId, String parentFileId, String fileId) {
        return pathBuilder(template)
                .buildAndExpand(pathVariables(driveId, parentFileId, fileId))
                .toUri();
    }

    private UriComponentsBuilder pathBuilder(String template) {
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + template)
                .encode();
    }

    private Map<String, String> pathVariables(String driveId, String parentFileId, String fileId) {
        Map<String, String> variables = new HashMap<>(PATH_VARIABLE_COUNT);
        variables.put("drive_id", nullToEmpty(driveId));
        variables.put("driveId", nullToEmpty(driveId));
        variables.put("parent_id", nullToEmpty(parentFileId));
        variables.put("parentId", nullToEmpty(parentFileId));
        variables.put("file_id", nullToEmpty(fileId));
        variables.put("fileId", nullToEmpty(fileId));
        return variables;
    }

    private String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private HttpMethod uploadMethod(WpsStoreRequest storeRequest) {
        if (!UPLOAD_METHOD.name().equalsIgnoreCase(storeRequest.getMethod())) {
            LOGGER.warn("拒绝WPS上传请求方法 请求方法={}", storeRequest.getMethod());
            throw WpsClientSupport.upstreamError(null);
        }
        return UPLOAD_METHOD;
    }

    private void validateUploadUrl(String url) {
        URI uri = uploadUri(url);
        if (!isAllowedUploadUri(uri)) {
            LOGGER.warn("拒绝WPS上传地址 主机={} 协议={} 是否包含用户信息={} 是否包含片段={}",
                    uri.getHost(),
                    uri.getScheme(),
                    Boolean.valueOf(uri.getUserInfo() != null),
                    Boolean.valueOf(uri.getFragment() != null));
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

    private String sha256Prefix(String sha256) {
        if (sha256 == null || sha256.length() <= HASH_PREFIX_LENGTH) {
            return sha256;
        }
        return sha256.substring(0, HASH_PREFIX_LENGTH);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
