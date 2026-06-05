package com.wps.yundoc.common.crypto;

/**
 * 网关内部安全算法标识。
 *
 * @author WPS
 * @date 2026-06-05 00:00:00
 */
public final class YundocCryptoAlgorithms {

    public static final String HMAC_SHA256 = "HMAC-SHA256";
    public static final String HMAC_SM3 = "HMAC-SM3";
    public static final String JWT_HS256 = "HS256";
    public static final String JWT_HSM3 = "HSM3";

    private YundocCryptoAlgorithms() {
    }
}
