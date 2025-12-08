package danexcodr.ai.pattern;

import java.util.*;

/**
 * Represents a higher-order pattern composed of other patterns (via their IDs) and words.
 * Now extends StructuralPattern so it can be processed by the PatternFamilyBuilder.
 * Tracks concrete fillers for variable slots.
 */
public class CompositePattern extends StructuralPattern {
    
    // Maps placeholder (e.g. "[1]", "[2]") to concrete values (e.g. "mammals")
    private Map<String, Set<String>> variableFillers = new TreeMap<String, Set<String>>();

    public CompositePattern(String id, List<String> slots) {
        // Pass "Composite" as the pattern type/ID, and slots as the structural elements
        super(id, slots);
    }
    
    public CompositePattern(List<String> slots) {
        this("CP" + System.currentTimeMillis(), slots);
    }
    
    public List<String> getCompositionSlots() {
        // Map to the parent's structural slots
        return getStructuralSlots();
    }
    
    public void addVariableFiller(String placeholder, String value) {
        if (!variableFillers.containsKey(placeholder)) {
            variableFillers.put(placeholder, new TreeSet<String>());
        }
        variableFillers.get(placeholder).add(value);
        updateTimestamp();
    }
    
    public Map<String, Set<String>> getVariableFillers() {
        return variableFillers;
    }
    
    @Override
    public boolean matchesSequence(List<String> sequence) {
        // Basic containment check
        return sequence.containsAll(getStructuralSlots());
    }
}