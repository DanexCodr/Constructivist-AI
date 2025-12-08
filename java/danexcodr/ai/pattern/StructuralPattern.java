package danexcodr.ai.pattern;

import java.util.List;

public class StructuralPattern extends AbstractPattern {
    private String patternType;
    private List<String> structuralSlots;
    private boolean isCommutative = false;
    
    // Keep existing constructor
    public StructuralPattern(String patternType, List<String> structuralSlots) {
        super(patternType);
        this.patternType = patternType;
        this.structuralSlots = structuralSlots;
    }
    
    public String getPatternType() { return patternType; }
    public List<String> getStructuralSlots() { return structuralSlots; }
    public boolean isCommutative() { return isCommutative; }
    
    public void setCommutative(boolean commutative) { 
        this.isCommutative = commutative; 
        updateTimestamp();
    }
    
    @Override
    public boolean matchesSequence(List<String> sequence) {
        if (isCommutative) {
            return sequence.containsAll(structuralSlots);
        } else {
            return sequence.equals(structuralSlots);
        }
    }
}