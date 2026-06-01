package com.wps.yundoc.wpsclient.infrastructure;

import com.wps.yundoc.wpsclient.application.WpsCommitUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateDriveRequest;
import com.wps.yundoc.wpsclient.application.WpsCreateFolderRequest;
import com.wps.yundoc.wpsclient.application.WpsDrive;
import com.wps.yundoc.wpsclient.application.WpsDriveList;
import com.wps.yundoc.wpsclient.application.WpsDriveListRequest;
import com.wps.yundoc.wpsclient.application.WpsFileChildrenRequest;
import com.wps.yundoc.wpsclient.application.WpsFileClient;
import com.wps.yundoc.wpsclient.application.WpsFileItem;
import com.wps.yundoc.wpsclient.application.WpsFileList;
import com.wps.yundoc.wpsclient.application.WpsFileListRequest;
import com.wps.yundoc.wpsclient.application.WpsRequestUploadRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadFileRequest;
import com.wps.yundoc.wpsclient.application.WpsUploadInfo;

/**
 * MockWpsFileClientAdapter component.
 *
 * @author WPS
 */
class MockWpsFileClientAdapter implements WpsFileClient {

    private final MockWpsClient delegate;

    MockWpsFileClientAdapter(MockWpsClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public WpsFileList listFiles(WpsFileListRequest request) {
        return delegate.listFiles(request);
    }

    @Override
    public WpsDriveList listDrives(WpsDriveListRequest request) {
        return delegate.listDrives(request);
    }

    @Override
    public WpsDrive createDrive(WpsCreateDriveRequest request) {
        return delegate.createDrive(request);
    }

    @Override
    public WpsFileList listChildren(WpsFileChildrenRequest request) {
        return delegate.listChildren(request);
    }

    @Override
    public WpsFileItem createFolder(WpsCreateFolderRequest request) {
        return delegate.createFolder(request);
    }

    @Override
    public WpsUploadInfo requestUpload(WpsRequestUploadRequest request) {
        return delegate.requestUpload(request);
    }

    @Override
    public void uploadFile(WpsUploadFileRequest request) {
        delegate.uploadFile(request);
    }

    @Override
    public WpsFileItem commitUpload(WpsCommitUploadRequest request) {
        return delegate.commitUpload(request);
    }
}
