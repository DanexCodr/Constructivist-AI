package danexcodr.ai.pattern;

import java.util.*;

public class Structure extends Abstract {
    private Data data;
    private boolean isCommutative = false;
    
    public Structure(String patternType) {
        super(patternType);
        this.data = new Data();
    }
    
    public Data getData() {
        return data;
    }
    
    public boolean isCommutative() {
        return isCommutative;
    }
    
    public void setCommutative(boolean commutative) {
        this.isCommutative = commutative;
        updateTimestamp();
    }
    
    @Override
    public boolean matchesSequence(List<String> sequence) {
        if (sequence == null) {
            return false;
        }
        
        if (isCommutative) {
            // For commutative patterns, collect all required tokens
            Set<String> requiredTokens = new HashSet<String>();
            for (int i = 0; i < data.size(); i++) {
                if (data.isToken(i)) {
                    requiredTokens.add(data.getTokenAt(i));
                } else if (data.isAlias(i)) {
                    requiredTokens.add(data.getAliasAt(i));
                }
                // Placeholders and PF tokens don't add requirements
            }
            return sequence.containsAll(requiredTokens);
        } else {
            // For non-commutative patterns, exact match with placeholder handling
            if (sequence.size() != data.size()) {
                return false;
            }
            
            for (int i = 0; i < data.size(); i++) {
                String token = sequence.get(i);
                
                if (data.isPlaceholder(i)) {
                    // Placeholder matches any token
                    continue;
                }
                
                if (data.isPFToken(i)) {
                    if (!data.getPFTokenAt(i).equals(token)) {
                        return false;
                    }
                    continue;
                }
                
                if (data.isToken(i)) {
                    if (!data.getTokenAt(i).equals(token)) {
                        return false;
                    }
                    continue;
                }
                
                if (data.isAlias(i)) {
                    if (!data.getAliasAt(i).equals(token)) {
                        return false;
                    }
                    continue;
                }
            }
            return true;
        }
    }
}