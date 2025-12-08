package danexcodr.ai.time;

/** * This will be used alongside BitDT
    * in the future for persistent
    * timestamping storage
    * **/

public interface Timestamp {
    long getCreatedAt();
    long getLastModifiedAt();
    void updateTimestamp();
}
