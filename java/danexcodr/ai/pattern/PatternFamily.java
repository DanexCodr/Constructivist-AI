package danexcodr.ai.pattern;

import java.util.*;
import java.util.Map.Entry;
import danexcodr.ai.time.Timestamp;

public class PatternFamily extends AbstractPattern {
    private Map<String, Set<String>> aliases = new LinkedHashMap<String, Set<String>>();
    private Map<String, String> wordToAlias = new HashMap<String, String>();
    private Set<StructuralPattern> memberPatterns = new HashSet<StructuralPattern>();
    private Set<List<String>> aliasedBasePatterns = new HashSet<List<String>>();
    
    private boolean isLogical = false;
    private long createdAt;
    private long lastModifiedAt;
    
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
            // Do not create aliases for Synthetic Content (PF tokens)
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

    public void addPattern(StructuralPattern sp) {
        if (memberPatterns.add(sp)) {
            this.frequency += sp.getFrequency();
            
            List<String> aliased = new ArrayList<String>();
            for (String token : sp.getStructuralSlots()) {
                if (wordToAlias.containsKey(token)) {
                    aliased.add(wordToAlias.get(token));
                } else {
                    aliased.add(token);
                }
            }
            aliasedBasePatterns.add(aliased);
            
            addConcreteExample(sp.getStructuralSlots());
            updateTimestamp();
        }
    }
    
    @Override
    public boolean matchesSequence(List<String> sequence) {
        for (StructuralPattern pattern : memberPatterns) {
            if (pattern.matchesSequence(sequence)) {
                return true;
            }
        }
        return false;
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
                    removed.startsWith("PF") ||
                    removed.endsWith("?")) {
                    return null;
                }
                
                List<String> mergedPattern = new ArrayList<String>(longer);
                mergedPattern.set(i, removed + "?");
                return mergedPattern;
            }
        }
        return null;
    }

    private String formatPattern(List<String> pattern) {
        StringBuilder sb = new StringBuilder("     ");
        for (int i = 0; i < pattern.size(); i++) {
            String token = pattern.get(i);
            
            // FIX: Box PF tokens for display
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

    public Map<String, Set<String>> getAliases() { return aliases; }
    public Map<String, String> getWordToAlias() { return wordToAlias; }
    public Set<StructuralPattern> getMemberPatterns() { return memberPatterns; }
    public Set<List<String>> getAliasedBasePatterns() { return aliasedBasePatterns; }
    
    public boolean isLogical() { return isLogical; }
    public void setLogical(boolean logical) { 
        this.isLogical = logical; 
        updateTimestamp();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--").append(id); 
        if (isLogical) {
            sb.append(" [LOGICAL]");
        }
        sb.append("\n\n");
        
        for (Entry<String, Set<String>> aliasEntry : aliases.entrySet()) {
            sb.append(String.format(" %-2s | %s\n", aliasEntry.getKey(), aliasEntry.getValue()));
        }
        
        List<String> merged = getMergedPatterns();
        for (String patternLine : merged) {
            sb.append(patternLine).append("\n");
        }

        sb.append("    Total frequency: ").append(frequency).append("\n");
        return sb.toString();
    }
}