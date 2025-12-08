package danexcodr.ai.pattern;

import java.util.*;

public class RelationPattern extends AbstractPattern {

    // terms
    private String T1;
    private String T2;
    
    // NEW: Base terms that originally formed T1/T2, used for contextual unwrapping.
    private Set<String> baseTermsT1 = new HashSet<String>();
    private Set<String> baseTermsT2 = new HashSet<String>();
    
    private Set<List<String>> observedStructures = new HashSet<List<String>>();
    
    private PatternFamily family = null;
    
    private boolean isCommutative = false;
    
    public RelationPattern(String id, String T1, String T2) {
        super(id);
        this.T1 = T1;
        this.T2 = T2;
    }
    
    public RelationPattern(String T1, String T2) {
        this("RP" + System.currentTimeMillis(), T1, T2);
    }
    
    public String getT1() { return T1; }
    public String getT2() { return T2; }
    public Set<List<String>> getObservedStructures() { return observedStructures; }
    
    // NEW Accessors
    public Set<String> getBaseTermsT1() { return baseTermsT1; }
    public void setBaseTermsT1(Set<String> terms) { 
        this.baseTermsT1.clear();
        this.baseTermsT1.addAll(terms);
        updateTimestamp(); 
    }
    
    public Set<String> getBaseTermsT2() { return baseTermsT2; }
    public void setBaseTermsT2(Set<String> terms) { 
        this.baseTermsT2.clear();
        this.baseTermsT2.addAll(terms);
        updateTimestamp(); 
    }
    
    public void setFamily(PatternFamily family) { 
        this.family = family; 
        updateTimestamp();
    }
    public PatternFamily getFamily() { return this.family; }
    
    public void setCommutative(boolean commutative) { 
        this.isCommutative = commutative; 
        updateTimestamp();
    }
    public boolean isCommutative() { return this.isCommutative; }
    
    @Override
    public boolean matchesSequence(List<String> sequence) {
        return sequence.contains(T1) && sequence.contains(T2);
    }
}