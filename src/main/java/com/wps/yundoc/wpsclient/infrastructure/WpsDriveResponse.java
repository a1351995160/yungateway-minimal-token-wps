package com.wps.yundoc.wpsclient.infrastructure;

/**
 * WpsDriveResponse component.
 *
 * @author WPS
 */
class WpsDriveResponse implements WpsEnvelope<DriveData> {

    private Integer code;
    private DriveData data;

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public DriveData getData() {
        return data;
    }

    public void setData(DriveData data) {
        this.data = data;
    }
}
