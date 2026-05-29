package com.wps.yundoc.capability.apppreview.api;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AppPreviewRequest {

    @Valid
    @NotNull
    private Source source;

    @Valid
    @NotNull
    private Options options;

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public static class Source {

        @NotBlank
        @Size(max = 32)
        private String type;

        @NotBlank
        @Size(max = 128)
        private String fileId;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }
    }

    public static class Options {

        @Min(60)
        @Max(86400)
        private int expireSeconds;

        public int getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(int expireSeconds) {
            this.expireSeconds = expireSeconds;
        }
    }
}
