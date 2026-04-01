package danexcodr.ai.pattern;

import danexcodr.ai.core.SlotFlag;

import java.util.*;
import java.util.Map.Entry;

public class PatternFamily extends Abstract {
    private Map<String, Set<String>> aliases = new LinkedHashMap<String, Set<String>>();
    private Map<String, String> tokenToAlias = new HashMap<String, String>();
    private Set<Structure> memberPatterns = new HashSet<Structure>();
    private Set<Data> aliasedBasePatterns = new HashSet<Data>();
    
    private long createdAt;
    private long lastModifiedAt;
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
    
    public void buildAliases(Set<String> familyTokens, Map<String, Set<String>> structuralEquivalents) {
        Set<String> processedTokens = new HashSet<String>();
        int aliasCount = 0;
        
        List<String> sortedFamilyTokens = new ArrayList<String>(familyTokens);
        Collections.sort(sortedFamilyTokens);
        
        for (String token : sortedFamilyTokens) {
            if (token.startsWith("PF")) continue;

            if (processedTokens.contains(token)) continue;
            
            String alias = "[S" + aliasCount++ + "]";
            Set<String> equivalentSet = new HashSet<String>();
            if (structuralEquivalents.containsKey(token)) {
                 equivalentSet.addAll(structuralEquivalents.get(token));
            }
            equivalentSet.add(token);

            aliases.put(alias, equivalentSet);
            
            for (String eqToken : equivalentSet) {
                if (familyTokens.contains(eqToken)) {
                    tokenToAlias.put(eqToken, alias);
                    processedTokens.add(eqToken);
                }
            }
        }
        updateTimestamp();
    }

    public void updateAliases(Map<String, Set<String>> structuralEquivalents) {
        Set<String> familyTokens = new HashSet<String>();
        
        for (Set<String> tokens : aliases.values()) {
            if (tokens != null) {
                familyTokens.addAll(tokens);
            }
        }
        
        familyTokens.addAll(tokenToAlias.keySet());
        
        for (Structure sp : memberPatterns) {
            Data data = sp.getData();
            for (int i = 0; i < data.size(); i++) {
                if (data.isToken(i)) {
                    familyTokens.add(data.getTokenAt(i));
                } else if (data.isAlias(i)) {
                    familyTokens.add(data.getAliasAt(i));
                }
            }
        }
        
        buildAliases(familyTokens, structuralEquivalents);
    }

    public void addAndMergePatterns(List<Structure> newPatterns) {
        boolean updated = false;
        
        for (Structure newPattern : newPatterns) {
            if (!containsEquivalentPattern(newPattern)) {
                addPattern(newPattern);
                updated = true;
            } else {
                Structure existing = findEquivalentPattern(newPattern);
                if (existing != null) {
                    existing.setFrequency(existing.getFrequency() + 1);
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
        
        Data data1 = p1.getData();
        Data data2 = p2.getData();
        
        if (data1.size() != data2.size()) {
            return false;
        }
        
        for (int i = 0; i < data1.size(); i++) {
            if (data1.isPlaceholder(i) != data2.isPlaceholder(i)) return false;
            if (data1.isToken(i) != data2.isToken(i)) return false;
            if (data1.isPFToken(i) != data2.isPFToken(i)) return false;
            if (data1.isAlias(i) != data2.isAlias(i)) return false;
            
            if (data1.isToken(i) && !data1.getTokenAt(i).equals(data2.getTokenAt(i))) return false;
            if (data1.isPFToken(i) && !data1.getPFTokenAt(i).equals(data2.getPFTokenAt(i))) return false;
            if (data1.isAlias(i) && !data1.getAliasAt(i).equals(data2.getAliasAt(i))) return false;
            if (data1.isPlaceholder(i) && data1.getPlaceholderAt(i) != data2.getPlaceholderAt(i)) return false;
        }
        
        return true;
    }

    public void addPattern(Structure sp) {
        if (memberPatterns.add(sp)) {
            this.frequency += 1;
            
            Data aliased = getAliasedData(sp);
            aliasedBasePatterns.add(aliased);
            
            updateTimestamp();
        }
    }
    
    public Data getAliasedData(Structure sp) {
        Data data = sp.getData();
        Data aliased = new Data();
        
        for (int i = 0; i < data.size(); i++) {
            if (data.isPlaceholder(i)) {
                aliased.addPlaceholder(data.getPlaceholderAt(i));
            } else if (data.isPFToken(i)) {
                aliased.addPFToken(data.getPFTokenAt(i));
            } else if (data.isToken(i)) {
                String token = data.getTokenAt(i);
                if (tokenToAlias.containsKey(token)) {
                    aliased.addAlias(tokenToAlias.get(token));
                } else {
                    aliased.addToken(token);
                }
            } else if (data.isAlias(i)) {
                String alias = data.getAliasAt(i);
                if (tokenToAlias.containsKey(alias)) {
                    aliased.addAlias(tokenToAlias.get(alias));
                } else {
                    aliased.addAlias(alias);
                }
            }
        }
        
        return aliased;
    }
    
    public void addRelationPattern(String relationPatternId) {
        if (relationPatternIds.add(relationPatternId)) {
            updateTimestamp();
        }
    }
    
    public Set<String> getRelationPatternIds() {
        return new HashSet<String>(relationPatternIds);
    }
    
    public boolean matchesSequence(List<String> sequence) {
        for (Structure pattern : memberPatterns) {
            if (pattern.matchesSequence(sequence)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean canGenerateFor(String term1, String term2, boolean requireCommutative) {
        for (Structure sp : memberPatterns) {
            if (requireCommutative && !sp.isCommutative()) {
                continue;
            }
            
            Data data = sp.getData();
            boolean hasTerm1Slot = false;
            boolean hasTerm2Slot = false;
            
            for (int i = 0; i < data.size(); i++) {
                if (data.isPlaceholder(i)) {
                    SlotFlag flag = data.getPlaceholderAt(i);
                    if (flag == SlotFlag._1 || flag == SlotFlag._C) {
                        hasTerm1Slot = true;
                    }
                    if (flag == SlotFlag._2 || flag == SlotFlag._C) {
                        hasTerm2Slot = true;
                    }
                    if (flag == SlotFlag._X) {
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
    
    private void rebuildAliasedBasePatterns() {
        aliasedBasePatterns.clear();
        for (Structure sp : memberPatterns) {
            aliasedBasePatterns.add(getAliasedData(sp));
        }
    }
    
    public Data getAliasedSlots(Structure sp) {
        return getAliasedData(sp);
    }
    
    public Map<String, Set<String>> getAliases() { 
        return aliases; 
    }
    
    public Map<String, String> getTokenToAlias() { 
        return tokenToAlias; 
    }
    
    public Set<Structure> getMemberPatterns() { 
        return memberPatterns; 
    }
    
    public Set<Data> getAliasedBasePatterns() { 
        return aliasedBasePatterns; 
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--").append(id); 
        sb.append("\n\n");
        
        for (Entry<String, Set<String>> aliasEntry : aliases.entrySet()) {
            sb.append(String.format(" %-2s | %s\n", aliasEntry.getKey(), aliasEntry.getValue()));
        }
        
        for (Data data : aliasedBasePatterns) {
            sb.append("     ");
            for (int i = 0; i < data.size(); i++) {
                if (i > 0) sb.append(", ");
                if (data.isPlaceholder(i)) {
                    sb.append(data.getPlaceholderAt(i).toToken());
                } else if (data.isToken(i)) {
                    sb.append(data.getTokenAt(i));
                } else if (data.isPFToken(i)) {
                    sb.append("[").append(data.getPFTokenAt(i)).append("]");
                } else if (data.isAlias(i)) {
                    sb.append(data.getAliasAt(i));
                }
            }
            sb.append("\n");
        }

        sb.append("    Total frequency: ").append(frequency).append("\n");
        return sb.toString();
    }
}