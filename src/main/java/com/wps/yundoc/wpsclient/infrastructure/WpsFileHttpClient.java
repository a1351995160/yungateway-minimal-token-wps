package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WpsFileHttpClient implements WpsFileClient {

    private final WpsClientProperties properties;
    private final RestTemplate restTemplate;

    public WpsFileHttpClient(WpsClientProperties properties, RestTemplateBuilder builder) {
        this(properties, builder, restTemplate(properties, builder));
    }

    public WpsFileHttpClient(
            WpsClientProperties properties,
            RestTemplateBuilder builder,
            RestTemplate restTemplate) {
        Objects.requireNonNull(builder, "builder");
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public WpsFileList listFiles(WpsFileListRequest request) {
        try {
            WpsFileListResponse response = exchange(request);
            return toFileList(response);
        } catch (RestClientException ex) {
            throw upstreamError(ex);
        }
    }

    private WpsFileListResponse exchange(WpsFileListRequest request) {
        return restTemplate.exchange(
                fileListUrl(request),
                HttpMethod.GET,
                entity(request),
                WpsFileListResponse.class).getBody();
    }

    private WpsFileList toFileList(WpsFileListResponse response) {
        FileListData data = requireData(response);
        return new WpsFileList(toItems(data.getItems()), data.getNextCursor());
    }

    private FileListData requireData(WpsFileListResponse response) {
        if (!hasSuccessEnvelope(response)) {
            throw upstreamError(null);
        }
        if (response.getData() == null) {
            throw upstreamError(null);
        }
        return response.getData();
    }

    private List<WpsFileItem> toItems(List<FileListItemData> items) {
        List<WpsFileItem> result = new ArrayList<>();
        for (FileListItemData item : safeItems(items)) {
            result.add(toItem(item));
        }
        return result;
    }

    private List<FileListItemData> safeItems(List<FileListItemData> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    private WpsFileItem toItem(FileListItemData item) {
        return new WpsFileItem(
                item.getFileId(),
                item.getName(),
                item.getType(),
                item.isFolder(),
                item.getUpdatedAt());
    }

    private HttpEntity<Void> entity(WpsFileListRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(request.getAccessToken());
        return new HttpEntity<>(headers);
    }

    private String fileListUrl(WpsFileListRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseFileListUrl())
                .queryParam("parentFileId", request.getParentFileId())
                .queryParam("limit", Integer.valueOf(request.getLimit()));
        addCursor(builder, request.getCursor());
        return builder.toUriString();
    }

    private void addCursor(UriComponentsBuilder builder, String cursor) {
        if (hasText(cursor)) {
            builder.queryParam("cursor", cursor);
        }
    }

    private String baseFileListUrl() {
        return properties.getBaseUrl() + properties.getFileListPath();
    }

    private boolean hasSuccessEnvelope(WpsEnvelope<?> response) {
        if (response == null) {
            return false;
        }
        if (response.getCode() == null) {
            return false;
        }
        return response.getCode().intValue() == 0;
    }

    private boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        return !value.trim().isEmpty();
    }

    private YundocException upstreamError(Throwable cause) {
        return new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "WPS upstream error", cause);
    }

    private static RestTemplate restTemplate(WpsClientProperties properties, RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }
}
