package danexcodr.ai.pattern;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import danexcodr.ai.time.Timestamp;

public abstract class Abstract implements Pattern, Timestamp {
    protected int frequency;
    protected Set<List<String>> concreteExamples;
    protected String id;
    private long createdAt;
    private long lastModifiedAt;
    
    // FIXED: Default frequency is 0
    public Abstract() {
        this.frequency = 0;
        this.concreteExamples = new HashSet<List<String>>();
        this.createdAt = System.currentTimeMillis();
        this.lastModifiedAt = System.currentTimeMillis();
    }
    
    // Keep the parameterized constructor
    public Abstract(String id) {
        this();
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public int getFrequency() {
        return frequency;
    }
    
    @Override
    public void setFrequency(int frequency) {
        this.frequency = frequency;
        updateTimestamp();
    }
    
    @Override
    public void incrementFrequency() {
        this.frequency++;
        updateTimestamp();
    }
    
    @Override
    public Set<List<String>> getConcreteExamples() {
        return concreteExamples;
    }
    
    @Override
    public void addConcreteExample(List<String> example) {
        this.concreteExamples.add(example);
        updateTimestamp();
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
    
    public abstract boolean matchesSequence(List<String> sequence);
}