package danexcodr.ai.core;

import danexcodr.ai.pattern.*;
import java.util.*;

public class PatternMatcher {
    
    public static boolean isSubSequenceMatch(
            List<String> sequence, 
            int startIdx, 
            List<String> patternSlots, 
            PatternFamily family,
            Set<String> protectedWords) {
        
        for (int j = 0; j < patternSlots.size(); j++) {
            String slot = patternSlots.get(j);
            String token = sequence.get(startIdx + j);
            
            // FIX: Handle PF tokens specially
            if (slot.startsWith("PF")) {
                // PF tokens in pattern must match exact PF tokens in sequence
                if (!slot.equals(token)) {
                    return false;
                }
                continue;
            }
            
            if ((slot.equals("[1]") || slot.equals("[2]") || slot.equals("[X]")) 
                    && protectedWords.contains(token)) {
                return false;
            }
            
            if (slot.equals("[1]") || slot.equals("[2]") || slot.equals("[C]") || slot.equals("[X]")) {
                continue;
            }
            
            if (slot.equals(token)) {
                continue;
            }
            
            String tokenAlias = family.getWordToAlias().get(token);
            String slotAlias = family.getWordToAlias().get(slot);
            
            if (family.getAliases().containsKey(slot) && tokenAlias != null && tokenAlias.equals(slot)) {
                continue;
            }
            
            if (tokenAlias != null && slotAlias != null && tokenAlias.equals(slotAlias)) {
                continue;
            }
            
            return false;
        }
        return true;
    }
    
    public static boolean matchesSequenceWithFamily(
            List<String> sequence, 
            PatternFamily family, 
            Set<String> protectedWords) {
        
        for (StructuralPattern sp : family.getMemberPatterns()) {
            List<String> slots = sp.getStructuralSlots();
            for (int i = 0; i <= sequence.size() - slots.size(); i++) {
                if (isSubSequenceMatch(sequence, i, slots, family, protectedWords)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // NEW: Method to check if sequence contains a PF token pattern
    public static boolean containsPatternFamily(List<String> sequence, PatternFamily family) {
        for (StructuralPattern sp : family.getMemberPatterns()) {
            List<String> slots = sp.getStructuralSlots();
            // Check if sequence contains all slots (order doesn't matter for commutative)
            if (sp.isCommutative()) {
                Set<String> slotSet = new HashSet<String>(slots);
                Set<String> seqSet = new HashSet<String>(sequence);
                return seqSet.containsAll(slotSet);
            } else {
                // For non-commutative, check if sequence contains the pattern as subsequence
                for (int i = 0; i <= sequence.size() - slots.size(); i++) {
                    boolean match = true;
                    for (int j = 0; j < slots.size(); j++) {
                        if (!slots.get(j).equals(sequence.get(i + j))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) return true;
                }
            }
        }
        return false;
    }
}