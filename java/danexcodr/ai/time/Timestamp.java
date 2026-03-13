package danexcodr.ai.time;

public interface Timestamp {
    long getCreatedAt();
    long getLastModifiedAt();
    void updateTimestamp();
}