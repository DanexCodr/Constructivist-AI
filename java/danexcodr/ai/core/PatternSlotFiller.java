package danexcodr.ai.core;

import java.util.*;

public class PatternSlotFiller {
    
    public static List<Set<String>> fillPatternSlots(
            List<String> structuralSlots, 
            Map<String, Set<String>> aliases,
            Map<String, String> wordToAlias,
            String T1, 
            String T2,
            boolean useCommutative,
            boolean isFlipped) {
        
        List<Set<String>> slots = new ArrayList<Set<String>>();
        boolean hasC = false;
        
        // FIX: Don't flip if pattern is not commutative
        if (isFlipped && !useCommutative) {
            isFlipped = false;
        }
        
        String firstTerm = isFlipped ? T2 : T1;
        String secondTerm = isFlipped ? T1 : T2;
        
        for (String token : structuralSlots) {
            Set<String> slotOptions = new HashSet<String>();
            
            // FIX: Handle PF tokens
            if (token.startsWith("PF")) {
                // PF tokens can be filled with themselves
                slotOptions.add(token);
                slots.add(slotOptions);
                continue;
            }
            
            if (token.equals("[1]")) {
                slotOptions.add(firstTerm);
            } else if (token.equals("[2]")) {
                slotOptions.add(secondTerm);
            } else if (token.equals("[C]")) {
                if (!hasC) {
                    slotOptions.add(firstTerm);
                    hasC = true;
                } else {
                    slotOptions.add(secondTerm);
                }
            } else if (token.equals("[X]")) {
                if (T1.startsWith("PF")) {
                    slotOptions.add(T2);
                } else {
                    slotOptions.add(T1);
                }
            } else {
                // Handle structural words
                
                // Check if token IS an alias in this family (e.g., "[S0]")
                if (aliases.containsKey(token)) {
                    Set<String> familyEquivalents = aliases.get(token);
                    if (familyEquivalents != null) {
                        slotOptions.addAll(familyEquivalents);
                    }
                } 
                // Check if token has an alias in this family
                else if (wordToAlias.containsKey(token)) {
                    String alias = wordToAlias.get(token);
                    Set<String> familyEquivalents = aliases.get(alias);
                    if (familyEquivalents != null) {
                        slotOptions.addAll(familyEquivalents);
                    }
                }
                // Otherwise, just add the token itself
                else {
                    slotOptions.add(token);
                }
            }
            
            slots.add(slotOptions);
        }
        return slots;
    }
    
    public static List<String> createFinalPatternSlots(
            List<String> abstractPattern, 
            String t1, 
            String t2, 
            boolean isCommutative) {
        
        List<String> finalSlots = new ArrayList<String>();
        boolean t1IsPF = t1.startsWith("PF");
        boolean t2IsPF = t2.startsWith("PF");

        for (String slot : abstractPattern) {
            // FIX: Preserve PF tokens
            if (slot.startsWith("PF")) {
                finalSlots.add(slot);
                continue;
            }
            
            if (slot.equals("[1]")) {
                if (isCommutative) {
                    finalSlots.add("[C]");
                } else if (t2IsPF && !t1IsPF) {
                    finalSlots.add("[X]"); 
                } else {
                    finalSlots.add("[1]");
                }
            } else if (slot.equals("[2]")) {
                if (isCommutative) {
                    finalSlots.add("[C]");
                } else if (t1IsPF && !t2IsPF) {
                    finalSlots.add("[X]"); 
                } else {
                    finalSlots.add("[2]");
                }
            } else {
                finalSlots.add(slot); 
            }
        }
        return finalSlots;
    }
}