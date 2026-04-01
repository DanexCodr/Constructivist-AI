package danexcodr.ai;

public class Config {

  public static final int TRAVERSE_DEPTH_LIMIT = 6;

  public static final int CONCEPT_LIMIT = 3;

  public static final double MAX_SENSITIVITY = 0.5;
  
  public static final double FORGETTING_THRESHOLD = 0.5;

  public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss";

  public static final String MAIN_COMMANDS_TEXT =
      "Commands: [l]earn, [p]rocess, [a]nalyze, [g]enerate, [v]iew, [q]uit";
}
