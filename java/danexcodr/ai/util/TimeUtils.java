package danexcodr.ai.util;

import danexcodr.ai.Config;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
  private static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat(Config.TIMESTAMP_PATTERN);

  public static String format(long timestamp) {
    return TIMESTAMP_FORMAT.format(new Date(timestamp));
  }

  public static String format(Date date) {
    return TIMESTAMP_FORMAT.format(date);
  }

  public static long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  public static boolean isOlderThan(long timestamp, long maxAgeMillis) {
    return (System.currentTimeMillis() - timestamp) > maxAgeMillis;
  }
}
