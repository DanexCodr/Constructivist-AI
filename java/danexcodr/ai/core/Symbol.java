package danexcodr.ai.core;

import danexcodr.ai.time.Timestamp;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class Symbol implements Timestamp {
  public String token;
  public int frequency;
  public Set<String> relations;
  public Map<String, Integer> leftContext;
  public Map<String, Integer> rightContext;
  private long createdAt;
  private long lastModifiedAt;

  public Symbol(String token) {
    this.token = token;
    this.frequency = 1;
    this.relations = new HashSet<String>();
    this.leftContext = new HashMap<String, Integer>();
    this.rightContext = new HashMap<String, Integer>();
    this.createdAt = System.currentTimeMillis();
    this.lastModifiedAt = System.currentTimeMillis();
  }

  @Override
  public void updateTimestamp() {
    this.lastModifiedAt = System.currentTimeMillis();
  }

  @Override
  public long getCreatedAt() {
    return createdAt;
  }

  @Override
  public long getLastModifiedAt() {
    return lastModifiedAt;
  }
}
