package at.jku.dke.bigkgolap.api;

import org.apache.commons.codec.digest.DigestUtils;

public class Utils {
    private Utils() {
        //
    }

    public static String sha1(String str) {
        return DigestUtils.sha1Hex(str);
    }
}
