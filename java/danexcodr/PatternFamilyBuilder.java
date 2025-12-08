package danexcodr.ai;

import java.util.*;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.LinkedList;

import danexcodr.ai.pattern.*;

public class PatternFamilyBuilder {
    
    private Map<String, Set<String>> structuralEquivalents;
    private List<Pattern> allPatterns;
    
    public PatternFamilyBuilder(Map<String, Set<String>> structuralEquivalents, List<Pattern> allPatterns) {
        this.structuralEquivalents = structuralEquivalents;
        this.allPatterns = new ArrayList<Pattern>(allPatterns);
    }
    
    private static class Key {
        String token;
        boolean isCommutative;
        
        Key(String token, boolean isCommutative) {
            this.token = token;
            this.isCommutative = isCommutative;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key tokenKey = (Key) o;
            if (isCommutative != tokenKey.isCommutative) return false;
            return token.equals(tokenKey.token);
        }
        
        @Override
        public int hashCode() {
            int result = (token != null ? token.hashCode() : 0);
            result = 31 * result + (isCommutative ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return token + (isCommutative ? "[C]" : "[NC]");
        }
    }

    public List<PatternFamily> buildPatternFamilies() {
        Map<Key, Set<Key>> relatedTokens = new HashMap<Key, Set<Key>>();
        Set<Key> allStructural = new HashSet<Key>();

        for (Entry<String, Set<String>> entry : structuralEquivalents.entrySet()) {
            String W1 = entry.getKey();
            for (String W2 : entry.getValue()) {
                addRelation(W1, W2, true, relatedTokens, allStructural);
                addRelation(W1, W2, false, relatedTokens, allStructural);
            }
        }
        
        for (Pattern pattern : allPatterns) {
            if (pattern instanceof StructuralPattern) {
                StructuralPattern sp = (StructuralPattern) pattern;
                boolean isCommutative = sp.isCommutative();
                
                List<Key> tokensInPattern = new ArrayList<Key>();
                for (String token : sp.getStructuralSlots()) {
                    // FIX: Ignore [C] and [X] so they are not treated as structural words
                    if (token.equals("[1]") || token.equals("[2]") || 
                        token.equals("[C]") || token.equals("[X]")) {
                        continue;
                    }

                    Key k = new Key(token, isCommutative);
                    tokensInPattern.add(k);
                    allStructural.add(k);
                    if (!relatedTokens.containsKey(k)) relatedTokens.put(k, new HashSet<Key>());
                }
                
                for (int i = 0; i < tokensInPattern.size(); i++) {
                    for (int j = i + 1; j < tokensInPattern.size(); j++) {
                        Key W1 = tokensInPattern.get(i);
                        Key W2 = tokensInPattern.get(j);
                        relatedTokens.get(W1).add(W2);
                        relatedTokens.get(W2).add(W1);
                    }
                }
            }
        }

        List<PatternFamily> families = new ArrayList<PatternFamily>();
        Set<Key> visited = new HashSet<Key>();
        
        List<Key> sortedAllStructural = new ArrayList<Key>(allStructural);
        Collections.sort(sortedAllStructural, new Comparator<Key>() {
            @Override
            public int compare(Key o1, Key o2) {
                int tokenCompare = o1.token.compareTo(o2.token);
                if (tokenCompare != 0) return tokenCompare;
                return (o1.isCommutative == o2.isCommutative) ? 0 : (o1.isCommutative ? 1 : -1);
            }
        });
        
        for (Key start : sortedAllStructural) {
            if (visited.contains(start)) continue;

            PatternFamily family = new PatternFamily("TEMP");
            families.add(family);
            
            Set<String> component = new HashSet<String>();
            Set<Key> componentKeys = new HashSet<Key>();
            
            Queue<Key> queue = new LinkedList<Key>();
            queue.add(start);
            visited.add(start);
            componentKeys.add(start);
            component.add(start.token);

            while (!queue.isEmpty()) {
                Key currentToken = queue.poll();
                if (relatedTokens.containsKey(currentToken)) {
                    for (Key neighbor : relatedTokens.get(currentToken)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            componentKeys.add(neighbor);
                            component.add(neighbor.token);
                            queue.add(neighbor);
                        }
                    }
                }
            }
            
            family.buildAliases(component, structuralEquivalents);
            addPatternsToFamily(family, componentKeys);
        }

        Collections.sort(families, new Comparator<PatternFamily>() {
            @Override
            public int compare(PatternFamily f1, PatternFamily f2) {
                long t1 = getEarliestMemberCreation(f1);
                long t2 = getEarliestMemberCreation(f2);
                
                int timeCompare = Long.compare(t1, t2);
                if (timeCompare != 0) return timeCompare;
                
                return f1.getId().compareTo(f2.getId());
            }
        });

        int familyCount = 1;
        for (PatternFamily f : families) {
            f.setId("PF" + (familyCount++));
        }

        return families;
    }

    private long getEarliestMemberCreation(PatternFamily family) {
        long earliest = Long.MAX_VALUE;
        if (family.getMemberPatterns().isEmpty()) return family.getCreatedAt();

        for (StructuralPattern sp : family.getMemberPatterns()) {
            if (sp.getCreatedAt() < earliest) {
                earliest = sp.getCreatedAt();
            }
        }
        return earliest;
    }
    
    private void addRelation(String w1, String w2, boolean isCommutative, Map<Key, Set<Key>> relatedTokens, Set<Key> allStructural) {
        Key k1 = new Key(w1, isCommutative);
        Key k2 = new Key(w2, isCommutative);
        allStructural.add(k1);
        allStructural.add(k2);
        if (!relatedTokens.containsKey(k1)) relatedTokens.put(k1, new HashSet<Key>());
        if (!relatedTokens.containsKey(k2)) relatedTokens.put(k2, new HashSet<Key>());
        relatedTokens.get(k1).add(k2);
        relatedTokens.get(k2).add(k1);
    }
    
    private void addPatternsToFamily(PatternFamily family, Set<Key> componentKeys) {
        if (componentKeys.isEmpty()) return;
        
        boolean familyIsCommutative = componentKeys.iterator().next().isCommutative;
        
        Set<String> component = new HashSet<String>();
        for (Key k : componentKeys) {
            component.add(k.token);
        }
        
        for (Pattern pattern : allPatterns) {
            if (pattern instanceof StructuralPattern) {
                StructuralPattern sp = (StructuralPattern) pattern;
                
                if (sp.isCommutative() != familyIsCommutative) {
                    continue;
                }
                
                boolean belongs = true;
                boolean hasFamily = false;
                
                List<String> patternSlots = sp.getStructuralSlots();

                for (String token : patternSlots) {
                    // FIX: Ignore [C] and [X] here as well
                    if (token.equals("[1]") || token.equals("[2]") || 
                        token.equals("[C]") || token.equals("[X]")) continue;
                    
                    if (!component.contains(token)) {
                        belongs = false;
                        break;
                    }
                    hasFamily = true;
                }
                
                if (belongs && hasFamily) {
                    family.addPattern(sp);
                }
            }
        }
    }
}