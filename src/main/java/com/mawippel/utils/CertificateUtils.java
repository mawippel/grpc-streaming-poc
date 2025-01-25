package com.mawippel.utils;

import org.apache.commons.lang3.StringUtils;

public class CertificateUtils {

    /**
     * Returns the Certificate Name
     * @param fullCN the CN in the following format "CN=cert-name-here"
     * @return a String with the certificate name
     */
    public static String getCertCN(String fullCN) {
        if (StringUtils.isBlank(fullCN)) {
            return "";
        }

        String[] cnParts = fullCN.split("=");
        if (cnParts.length >= 2) {
            return cnParts[1]; // return the name after the '='
        }

        return "";
    }
}
