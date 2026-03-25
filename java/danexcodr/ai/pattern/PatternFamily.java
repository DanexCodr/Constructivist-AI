package danexcodr.ai.pattern;

import java.util.*;
import java.util.Map.Entry;
import danexcodr.ai.time.Timestamp;

public class PatternFamily extends Abstract {
    private Map<String, Set<String>> aliases = new LinkedHashMap<String, Set<String>>();
    private Map<String, String> wordToAlias = new HashMap<String, String>();
    private Set<Structure> memberPatterns = new HashSet<Structure>();
    private Set<List<String>> aliasedBasePatterns = new HashSet<List<String>>();
    
    private long createdAt;
    private long lastModifiedAt;
    
    // NEW: Store which relation patterns belong to this family
    private Set<String> relationPatternIds = new HashSet<String>();
    
    public PatternFamily(String id) {
        super(id);
        this.createdAt = System.currentTimeMillis();
        this.lastModifiedAt = System.currentTimeMillis();
    }

    public void setId(String id) {
        this.id = id;
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
    
    public void buildAliases(Set<String> familyWords, Map<String, Set<String>> structuralEquivalents) {
        Set<String> processedWords = new HashSet<String>();
        int aliasCount = 0;
        
        List<String> sortedFamilyWords = new ArrayList<String>(familyWords);
        Collections.sort(sortedFamilyWords);
        
        for (String word : sortedFamilyWords) {
            if (word.startsWith("PF")) continue;

            if (processedWords.contains(word)) continue;
            
            String alias = "[S" + aliasCount++ + "]";
            Set<String> equivalentSet = new HashSet<String>();
            if (structuralEquivalents.containsKey(word)) {
                 equivalentSet.addAll(structuralEquivalents.get(word));
            }
            equivalentSet.add(word);

            aliases.put(alias, equivalentSet);
            
            for (String eqWord : equivalentSet) {
                if (familyWords.contains(eqWord)) {
                    wordToAlias.put(eqWord, alias);
                    processedWords.add(eqWord);
                }
            }
        }
        updateTimestamp();
    }

    public void updateAliases(Map<String, Set<String>> structuralEquivalents) {
        Set<String> familyWords = new HashSet<String>();
        
        for (Set<String> words : aliases.values()) {
            if (words != null) {
                familyWords.addAll(words);
            }
        }
        
        familyWords.addAll(wordToAlias.keySet());
        
        for (Structure sp : memberPatterns) {
            for (String token : sp.getStructuralSlots()) {
                if (!token.equals("[1]") && !token.equals("[2]") && 
                    !token.equals("[C]") && !token.equals("[X]") && 
                    !token.startsWith("PF")) {
                    familyWords.add(token);
                }
            }
        }
        
        buildAliases(familyWords, structuralEquivalents);
    }

    // NEW: Add patterns and merge duplicates
    public void addAndMergePatterns(List<Structure> newPatterns) {
        boolean updated = false;
        
        for (Structure newPattern : newPatterns) {
            // Check if pattern already exists
            if (!containsEquivalentPattern(newPattern)) {
                addPattern(newPattern);
                updated = true;
            } else {
                // Update frequency if pattern exists
                Structure existing = findEquivalentPattern(newPattern);
                if (existing != null) {
                    existing.setFrequency(existing.getFrequency() + 1);
                    
                    // Also add concrete examples from new pattern
                    for (List<String> example : newPattern.getConcreteExamples()) {
                        existing.addConcreteExample(example);
                    }
                    
                    updated = true;
                }
            }
        }
        
        if (updated) {
            rebuildAliasedBasePatterns();
            updateTimestamp();
        }
    }
    
    private boolean containsEquivalentPattern(Structure pattern) {
        for (Structure existing : memberPatterns) {
            if (arePatternsEquivalent(existing, pattern)) {
                return true;
            }
        }
        return false;
    }
    
    private Structure findEquivalentPattern(Structure pattern) {
        for (Structure existing : memberPatterns) {
            if (arePatternsEquivalent(existing, pattern)) {
                return existing;
            }
        }
        return null;
    }
    
    private boolean arePatternsEquivalent(Structure p1, Structure p2) {
        if (p1.isCommutative() != p2.isCommutative()) {
            return false;
        }
        
        List<String> slots1 = p1.getStructuralSlots();
        List<String> slots2 = p2.getStructuralSlots();
        
        if (slots1.size() != slots2.size()) {
            return false;
        }
        
        for (int i = 0; i < slots1.size(); i++) {
            String s1 = slots1.get(i);
            String s2 = slots2.get(i);
            
            if (!s1.equals(s2)) {
                return false;
            }
        }
        
        return true;
    }

    // Add pattern to family
    public void addPattern(Structure sp) {
        if (memberPatterns.add(sp)) {
            this.frequency += 1;
            
            List<String> aliased = new ArrayList<String>();
            for (String token : sp.getStructuralSlots()) {
                if (wordToAlias.containsKey(token)) {
                    aliased.add(wordToAlias.get(token));
                } else {
                    aliased.add(token);
                }
            }
            aliasedBasePatterns.add(aliased);
            
            // Add concrete examples from the pattern
            for (List<String> example : sp.getConcreteExamples()) {
                addConcreteExample(example);
            }
            
            updateTimestamp();
        }
    }
    
    // NEW: Add relation pattern to this family
    public void addRelationPattern(String relationPatternId) {
        if (relationPatternIds.add(relationPatternId)) {
            updateTimestamp();
        }
    }
    
    // NEW: Get all relation patterns in this family
    public Set<String> getRelationPatternIds() {
        return new HashSet<String>(relationPatternIds);
    }
    
    @Override
    public boolean matchesSequence(List<String> sequence) {
        for (Structure pattern : memberPatterns) {
            if (pattern.matchesSequence(sequence)) {
                return true;
            }
        }
        return false;
    }
    
    // NEW: Check if this family can generate sequence for given terms
    public boolean canGenerateFor(String term1, String term2, boolean requireCommutative) {
        for (Structure sp : memberPatterns) {
            if (requireCommutative && !sp.isCommutative()) {
                continue;
            }
            
            // Check if pattern has slots that can accept our terms
            List<String> slots = sp.getStructuralSlots();
            boolean hasTerm1Slot = false;
            boolean hasTerm2Slot = false;
            
            for (String slot : slots) {
                if (slot.equals("[1]") || slot.equals("[2]") || slot.equals("[C]") || slot.equals("[X]")) {
                    if (slot.equals("[1]") || slot.equals("[C]")) {
                        hasTerm1Slot = true;
                    }
                    if (slot.equals("[2]") || slot.equals("[C]")) {
                        hasTerm2Slot = true;
                    }
                    if (slot.equals("[X]")) {
                        // [X] can be either term
                        hasTerm1Slot = hasTerm2Slot = true;
                    }
                }
            }
            
            if (hasTerm1Slot && hasTerm2Slot) {
                return true;
            }
        }
        return false;
    }
    
    // NEW: Rebuild aliased base patterns after merging
    private void rebuildAliasedBasePatterns() {
        aliasedBasePatterns.clear();
        for (Structure sp : memberPatterns) {
            List<String> aliased = new ArrayList<String>();
            for (String token : sp.getStructuralSlots()) {
                if (wordToAlias.containsKey(token)) {
                    aliased.add(wordToAlias.get(token));
                } else {
                    aliased.add(token);
                }
            }
            aliasedBasePatterns.add(aliased);
        }
    }
    
    public List<String> getMergedPatterns() {
        List<List<String>> currentPatterns = new ArrayList<List<String>>(aliasedBasePatterns);
        
        boolean mergeOccurred = true;
        while (mergeOccurred) {
            mergeOccurred = false;
            List<List<String>> nextPatterns = new ArrayList<List<String>>();
            Set<Integer> mergedIndices = new HashSet<Integer>();
            
            Collections.sort(currentPatterns, new Comparator<List<String>>() {
                @Override
                public int compare(List<String> o1, List<String> o2) {
                    int lenCompare = Integer.compare(o1.size(), o2.size());
                    if (lenCompare != 0) return lenCompare;
                    return o1.toString().compareTo(o2.toString());
                }
            });

            for (int i = 0; i < currentPatterns.size(); i++) {
                if (mergedIndices.contains(i)) continue;
                
                List<String> basePattern = new ArrayList<String>(currentPatterns.get(i));
                
                for (int j = i + 1; j < currentPatterns.size(); j++) {
                    if (mergedIndices.contains(j)) continue;
                    
                    List<String> p2 = currentPatterns.get(j);
                    List<String> merged = tryMerge(basePattern, p2);
                    
                    if (merged != null) {
                        basePattern = merged;
                        mergedIndices.add(j);
                        mergeOccurred = true;
                    }
                }
                
                nextPatterns.add(basePattern);
                mergedIndices.add(i);
            }
            
            currentPatterns = nextPatterns;
        }
        
        List<String> finalPatterns = new ArrayList<String>();
        for (List<String> finalPattern : currentPatterns) {
            finalPatterns.add(formatPattern(finalPattern));
        }

        return finalPatterns;
    }
    
    public List<String> getAliasedSlots(Structure sp) {
        List<String> aliased = new ArrayList<String>();
        for (String token : sp.getStructuralSlots()) {
            if (wordToAlias.containsKey(token)) {
                aliased.add(wordToAlias.get(token));
            } else {
                aliased.add(token);
            }
        }
        return aliased;
    }
    
    private List<String> tryMerge(List<String> p1, List<String> p2) {
        List<String> merged = attemptInsertionMerge(p1, p2);
        if (merged != null) return merged;
        
        merged = attemptInsertionMerge(p2, p1);
        if (merged != null) return merged;

        return null;
    }
    
    private List<String> attemptInsertionMerge(List<String> shorter, List<String> longer) {
        if (shorter.size() != longer.size() - 1 || shorter.equals(longer)) {
            return null;
        }

        for (int i = 0; i < longer.size(); i++) {
            List<String> temp = new ArrayList<String>(longer);
            String removed = temp.remove(i);
            
            if (temp.equals(shorter)) {
                if (removed.equals("[C]") || 
                    removed.equals("[1]") || 
                    removed.equals("[2]") || 
                    removed.equals("[X]") ||
                    removed.startsWith("PF")) {
                    return null;
                }
                
                boolean appearsInAll = true;
                for (Structure member : this.memberPatterns) {
                    boolean found = false;
                    for (String memberToken : member.getStructuralSlots()) {
                        if (memberToken.equals(removed)) {
                            found = true;
                            break;
                        }
                        // Check if memberToken has an alias that matches removed's alias
                        String memberAlias = wordToAlias.get(memberToken);
                        String removedAlias = wordToAlias.get(removed);
                        if (memberAlias != null && removedAlias != null && 
                            memberAlias.equals(removedAlias)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        appearsInAll = false;
                        break;
                    }
                }
                
                if (appearsInAll) {
                    return null; // Word appears in all patterns, not optional
                }
                
                List<String> mergedPattern = new ArrayList<String>(longer);
                mergedPattern.set(i, removed + "?");
                return mergedPattern;
            }
        }
        return null;
    }

    private String formatPattern(List<String> pattern) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pattern.size(); i++) {
            String token = pattern.get(i);
            
            if (token.startsWith("PF")) {
                sb.append("[").append(token).append("]");
            } else {
                sb.append(token);
            }
            
            if (i < pattern.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public Map<String, Set<String>> getAliases() { 
        return aliases; 
    }
    
    public Map<String, String> getWordToAlias() { return wordToAlias; }
    public Set<Structure> getMemberPatterns() { return memberPatterns; }
    public Set<List<String>> getAliasedBasePatterns() { return aliasedBasePatterns; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--").append(id); 
        sb.append("\n\n");
        
        for (Entry<String, Set<String>> aliasEntry : aliases.entrySet()) {
            sb.append(String.format(" %-2s | %s\n", aliasEntry.getKey(), aliasEntry.getValue()));
        }
        
        List<String> merged = getMergedPatterns();
        for (String patternLine : merged) {
            sb.append("     ").append(patternLine).append("\n");
        }

        sb.append("    Total frequency: ").append(frequency).append("\n");
        return sb.toString();
    }
}