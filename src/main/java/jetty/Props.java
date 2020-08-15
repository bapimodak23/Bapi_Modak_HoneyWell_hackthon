package jetty;

import org.apache.cxf.common.util.StringUtils;

public class Props {

    public static String getString(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        return System.getenv(key);
    }

    public static String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    public static int getInteger(String key, int defaultValue) {
        String value = getString(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            //nop
        }
        return defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return "1".equals(value) || "true".compareToIgnoreCase(value) == 0;
    }
}
