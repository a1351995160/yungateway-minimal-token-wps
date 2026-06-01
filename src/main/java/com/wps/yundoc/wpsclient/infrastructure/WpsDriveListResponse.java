package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsDriveListResponse component.
 *
 * @author WPS
 */
class WpsDriveListResponse implements WpsEnvelope<DriveListData> {

    private Integer code;
    private DriveListData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public DriveListData getData() {
        return data;
    }

    public void setData(DriveListData data) {
        this.data = data;
    }
}
