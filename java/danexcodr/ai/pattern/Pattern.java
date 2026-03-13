package danexcodr.ai.pattern;

import java.util.List;
import java.util.Set;

public interface Pattern {
    int getFrequency();
    void setFrequency(int frequency);
    void incrementFrequency();
    Set<List<String>> getConcreteExamples();
    void addConcreteExample(List<String> example);
    boolean matchesSequence(List<String> sequence);
    String getId();
}