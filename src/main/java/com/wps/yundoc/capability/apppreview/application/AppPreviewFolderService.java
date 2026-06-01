package com.wps.yundoc.capability.apppreview.application;

import com.wps.yundoc.capability.apppreview.domain.AppPreviewFolder;
import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewFolderMapper;
import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewFolderPO;
import com.wps.yundoc.capability.apppreview.infrastructure.AppPreviewUploadProperties;
import com.wps.yundoc.common.error.YundocErrorCode;
import com.wps.yundoc.common.error.YundocException;
import com.wps.yundoc.common.util.Texts;
import com.wps.yundoc.wpsclient.application.WpsCreateDriveRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateFolderRequest;
import com.wps.yundoc.wpsclient.application.WpsDrive;
import com.wps.yundoc.wpsclient.application.WpsDriveList;
import com.wps.yundoc.wpsclient.application.WpsDriveListRequest;
import com.wps.yundoc.wpsclient.application.WpsFileChildrenRequest;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * AppPreviewFolderService component.
 *
 * @author WPS
 */
@Service
public class AppPreviewFolderService {

    private static final String DRIVE_STATUS_INUSE = "inuse";
    private static final int FOLDER_HASH_LENGTH = 12;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final AppPreviewFolderMapper folderMapper;
    private final WpsFileClient fileClient;
    private final AppPreviewUploadProperties properties;

    public AppPreviewFolderService(
            AppPreviewFolderMapper folderMapper,
            WpsFileClient fileClient,
            AppPreviewUploadProperties properties) {
        this.folderMapper = folderMapper;
        this.fileClient = fileClient;
        this.properties = properties;
    }

    public synchronized AppPreviewFolder ensureFolder(String businessSystemId, String accessToken) {
        WpsDrive drive = resolveDrive(accessToken);
        AppPreviewFolderPO existing = folderMapper.selectByBusinessSystemIdAndDriveId(
                businessSystemId,
                drive.getDriveId());
        if (existing != null) {
            return folder(existing);
        }
        return createOrRecoverFolder(businessSystemId, accessToken, drive);
    }

    private AppPreviewFolder createOrRecoverFolder(String businessSystemId, String accessToken, WpsDrive drive) {
        String folderName = folderName(businessSystemId);
        WpsFileItem folder = findFolder(accessToken, drive.getDriveId(), folderName);
        if (folder == null) {
            folder = createFolder(accessToken, drive.getDriveId(), folderName);
        }
        saveFolder(businessSystemId, drive.getDriveId(), folder);
        return new AppPreviewFolder(drive.getDriveId(), folder.getFileId(), folder.getName());
    }

    private WpsDrive resolveDrive(String accessToken) {
        if (Texts.hasText(properties.getDriveId())) {
            return new WpsDrive(properties.getDriveId().trim(), properties.getDriveName(), DRIVE_STATUS_INUSE, null);
        }
        WpsDrive drive = findDrive(accessToken);
        if (drive != null) {
            return drive;
        }
        return createDrive(accessToken);
    }

    private WpsDrive findDrive(String accessToken) {
        String pageToken = null;
        WpsDrive found = null;
        do {
            WpsDriveList list = fileClient.listDrives(new WpsDriveListRequest(
                    accessToken,
                    properties.getDrivePageSize(),
                    pageToken));
            found = selectDrive(list);
            pageToken = list.getNextPageToken();
        } while (found == null && Texts.hasText(pageToken));
        return found;
    }

    private WpsDrive selectDrive(WpsDriveList list) {
        for (WpsDrive drive : list.getItems()) {
            if (matchesDrive(drive)) {
                return drive;
            }
        }
        return null;
    }

    private boolean matchesDrive(WpsDrive drive) {
        if (!DRIVE_STATUS_INUSE.equals(drive.getStatus())) {
            return false;
        }
        return !Texts.hasText(properties.getDriveName())
                || properties.getDriveName().equals(drive.getName());
    }

    private WpsDrive createDrive(String accessToken) {
        if (!properties.isAutoCreateDrive()) {
            throw new YundocException(YundocErrorCode.WPS_UPSTREAM_ERROR, "WPS app drive is not initialized");
        }
        return fileClient.createDrive(new WpsCreateDriveRequest(
                accessToken,
                properties.getDriveName(),
                properties.getDriveSource(),
                properties.getDriveTotalQuota()));
    }

    private WpsFileItem findFolder(String accessToken, String driveId, String folderName) {
        String pageToken = null;
        WpsFileItem found = null;
        do {
            WpsFileList list = fileClient.listChildren(new WpsFileChildrenRequest(
                    accessToken,
                    driveId,
                    properties.getRootParentId(),
                    properties.getFileListPageSize(),
                    pageToken));
            found = selectFolder(list, folderName);
            pageToken = list.getNextCursor();
        } while (found == null && Texts.hasText(pageToken));
        return found;
    }

    private WpsFileItem selectFolder(WpsFileList list, String folderName) {
        for (WpsFileItem item : list.getItems()) {
            if (item.isFolder() && folderName.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    private WpsFileItem createFolder(String accessToken, String driveId, String folderName) {
        return fileClient.createFolder(new WpsCreateFolderRequest(
                accessToken,
                driveId,
                properties.getRootParentId(),
                folderName,
                properties.getFolderConflictBehavior()));
    }

    private void saveFolder(String businessSystemId, String driveId, WpsFileItem folder) {
        AppPreviewFolderPO po = new AppPreviewFolderPO();
        po.setBusinessSystemId(businessSystemId);
        po.setDriveId(driveId);
        po.setFolderId(folder.getFileId());
        po.setFolderName(folder.getName());
        folderMapper.upsert(po);
    }

    private AppPreviewFolder folder(AppPreviewFolderPO po) {
        return new AppPreviewFolder(po.getDriveId(), po.getFolderId(), po.getFolderName());
    }

    private String folderName(String businessSystemId) {
        return properties.getFolderNamePrefix()
                + safeBusinessSystemId(businessSystemId)
                + "-"
                + folderNameHash(businessSystemId);
    }

    private String safeBusinessSystemId(String businessSystemId) {
        return businessSystemId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
    }

    private String folderNameHash(String businessSystemId) {
        return hex(sha256(businessSystemId)).substring(0, FOLDER_HASH_LENGTH);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new YundocException(YundocErrorCode.INTERNAL_ERROR, "SHA-256 is unavailable", ex);
        }
    }

    private String hex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            result[index * 2] = HEX[value >>> 4];
            result[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(result);
    }
}
