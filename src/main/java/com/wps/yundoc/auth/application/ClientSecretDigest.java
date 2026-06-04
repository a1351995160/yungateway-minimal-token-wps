package com.wps.yundoc.auth.application;

/**
 * ClientSecretDigest 组件。
 *
 * @author WPS
 * @date 2026-06-02 08:53:49
 */
public class ClientSecretDigest {

    private final String digest;
    private final String salt;
    private final String algorithm;

    public ClientSecretDigest(String digest, String salt, String algorithm) {
        this.digest = digest;
        this.salt = salt;
        this.algorithm = algorithm;
    }

    public String getDigest() {
        return digest;
    }

    public String getSalt() {
        return salt;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
