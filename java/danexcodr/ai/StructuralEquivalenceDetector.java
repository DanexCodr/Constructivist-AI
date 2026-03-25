package danexcodr.ai;

import java.util.*;
import java.util.Map.Entry;

import danexcodr.ai.core.*;
import danexcodr.ai.pattern.*;

public class StructuralEquivalenceDetector {
    
    private Map<String, Symbol> symbols;
    private Set<String> allLearnedOptionals;
    
    public StructuralEquivalenceDetector(Map<String, Symbol> symbols, Set<String> allLearnedOptionals) {
        this.symbols = symbols;
        this.allLearnedOptionals = allLearnedOptionals;
    }
    
    public EquivalencePair createEquivalencePair(String w1, String w2) {
        return new EquivalencePair(w1, w2);
    }
    
    public static class EquivalencePair {
        public String w1;
        public String w2;
        
        EquivalencePair(String w1, String w2) {
            this.w1 = w1;
            this.w2 = w2;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            EquivalencePair that = (EquivalencePair) obj;
            
            return (w1.equals(that.w1) && w2.equals(that.w2)) ||
                   (w1.equals(that.w2) && w2.equals(that.w1));
        }
        
        @Override
        public int hashCode() {
            return w1.hashCode() + w2.hashCode();
        }
        
        @Override
        public String toString() {
            return w1 + " ~  " + w2;
        }
    }

    private Map<String, Set<List<String>>> extractPatternTemplates(List<Pattern> allPatterns) {
        Map<String, Set<List<String>>> templates = new HashMap<String, Set<List<String>>>();
        
        for (Pattern pattern : allPatterns) {
            if (pattern instanceof Structure) {
                Structure sp = (Structure) pattern;
                List<String> structuralSlots = sp.getStructuralSlots();
                
                List<String> template = new ArrayList<String>();
                for (String slot : structuralSlots) {
                    if (slot.equals("[1]") || slot.equals("[2]")) {
                        template.add(slot);
                    } else if (allLearnedOptionals.contains(slot)) {
                        template.add("[OPTIONAL]");
                    } else {
                        template.add("[STRUCTURAL]");
                    }
                }
                
                // --- FIX STARTS HERE ---
                // Old code:
                // String templateKey = template.toString(); 

                // New code: Include commutativity in the key to separate logic types
                String templateKey = template.toString() + "|" + sp.isCommutative();
                // --- FIX ENDS HERE ---

                if (!templates.containsKey(templateKey)) {
                    templates.put(templateKey, new HashSet<List<String>>());
                }
                templates.get(templateKey).add(structuralSlots);
            }
        }
        return templates;
    }

    private Map<String, Map<Integer, Set<String>>> analyzeTemplateSlots(Map<String, Set<List<String>>> templates) {
        Map<String, Map<Integer, Set<String>>> templateSlots = new HashMap<String, Map<Integer, Set<String>>>();
        
        for (Map.Entry<String, Set<List<String>>> entry : templates.entrySet()) {
            String templateKey = entry.getKey();
            Set<List<String>> concretePatterns = entry.getValue();
            
            if (concretePatterns.size() < 2) continue;
            
            Map<Integer, Set<String>> slotAnalysis = new HashMap<Integer, Set<String>>();
            
            int patternLength = getMaxPatternLength(concretePatterns);
            
            for (int i = 0; i < patternLength; i++) {
                Set<String> wordsAtPosition = new HashSet<String>();
                
                for (List<String> pattern : concretePatterns) {
                    if (i < pattern.size()) {
                        String word = pattern.get(i);
                        if (!word.equals("[1]") && !word.equals("[2]")) {
                            wordsAtPosition.add(word);
                        }
                    }
                }
                
                if (wordsAtPosition.size() > 1) {
                    slotAnalysis.put(i, wordsAtPosition);
                }
            }
            
            if (!slotAnalysis.isEmpty()) {
                templateSlots.put(templateKey, slotAnalysis);
            }
        }
        
        return templateSlots;
    }
    
    private int getMaxPatternLength(Set<List<String>> patterns) {
        int maxLength = 0;
        for (List<String> pattern : patterns) {
            if (pattern.size() > maxLength) {
                maxLength = pattern.size();
            }
        }
        return maxLength;
    }
    
    private Set<EquivalencePair> detectTemplateBasedEquivalents(List<Pattern> allPatterns) {
        Set<EquivalencePair> equivalents = new HashSet<EquivalencePair>();
        
        Map<String, Set<List<String>>> templates = extractPatternTemplates(allPatterns);
        Map<String, Map<Integer, Set<String>>> templateSlots = analyzeTemplateSlots(templates);
        
        for (Map.Entry<String, Map<Integer, Set<String>>> templateEntry : templateSlots.entrySet()) {
            String templateKey = templateEntry.getKey();
            Map<Integer, Set<String>> slotAnalysis = templateEntry.getValue();
            
            for (Map.Entry<Integer, Set<String>> slotEntry : slotAnalysis.entrySet()) {
                int position = slotEntry.getKey();
                Set<String> variableWords = slotEntry.getValue();
                
                if (variableWords.size() > 1) {
                    List<String> wordList = new ArrayList<String>(variableWords);
                    
                    for (int i = 0; i < wordList.size(); i++) {
                        for (int j = i + 1; j < wordList.size(); j++) {
                            String w1 = wordList.get(i);
                            String w2 = wordList.get(j);
                            
                            if (!areNeighbors(w1, w2)) {
                                equivalents.add(new EquivalencePair(w1, w2));
                            }
                        }
                    }
                }
            }
        }
        
        return equivalents;
    }
    
    public Set<EquivalencePair> detectStructuralEquivalents(List<List<String>> equivalentSequences, 
                                       Set<String> structuralWords, 
                                       String t1, String t2,
                                       List<Pattern> allPatterns) {
        
        Set<EquivalencePair> foundEquivalents = new HashSet<EquivalencePair>();
        
        Set<EquivalencePair> templateEquivalents = detectTemplateBasedEquivalents(allPatterns);
        foundEquivalents.addAll(templateEquivalents);
        
        Set<EquivalencePair> contentBasedEquivalents = detectContentBasedEquivalents(
            equivalentSequences, structuralWords, t1, t2, allPatterns);
        foundEquivalents.addAll(contentBasedEquivalents);
        
        return foundEquivalents;
    }
    
    private Set<EquivalencePair> detectContentBasedEquivalents(List<List<String>> equivalentSequences, 
                                           Set<String> structuralWords, 
                                           String t1, String t2,
                                           List<Pattern> allPatterns) {
        
        Set<EquivalencePair> foundEquivalents = new HashSet<EquivalencePair>();
        
        Map<Set<String>, Set<Structure>> existingFamilies = groupPatternsByStructuralFamily(allPatterns);
        
        if (existingFamilies.isEmpty()) {
            return foundEquivalents;
        }

        Set<String> currentContentWords = new HashSet<String>(Arrays.asList(t1, t2));

        Set<List<String>> currentPatternFamily = new HashSet<List<String>>();
        for (List<String> seq : equivalentSequences) {
            List<String> abstractPattern = SequenceTransformer.abstractSequence(seq, t1, t2); 
            currentPatternFamily.add(abstractPattern);
        }
        
        Set<String> currentStructuralSignature = extractStructuralSignature(currentPatternFamily);
        
        for (Entry<Set<String>, Set<Structure>> familyEntry : existingFamilies.entrySet()) {
            Set<String> existingStructuralSignature = familyEntry.getKey();
            
            if (haveSimilarStructuralSignature(currentStructuralSignature, existingStructuralSignature)) {
                for (List<String> currentPattern : currentPatternFamily) {
                    
                    List<String> flippedCurrent = SequenceTransformer.flipTermPattern(currentPattern);
                    boolean isCurrentCommutative = !currentPattern.equals(flippedCurrent) && currentPatternFamily.contains(flippedCurrent);

                    for (Structure existingPattern : familyEntry.getValue()) {
                        
                        if (existingPattern.isCommutative() != isCurrentCommutative) {
                            continue;
                        }

                        boolean shareContext = shareSemanticContext(existingPattern, currentPattern, currentContentWords);
                        if (!shareContext) {
                            continue; 
                        }
                        
                        boolean sameStructure = samePatternStructure(existingPattern.getStructuralSlots(), currentPattern);
                        
                        if (sameStructure) { 
                            findDifferingStructuralWords(existingPattern.getStructuralSlots(), currentPattern, foundEquivalents);
                        }
                    }
                }
            }
        }
        
        return foundEquivalents;
    }

    public boolean areNeighbors(String w1, String w2) {
        if (!symbols.containsKey(w1) || !symbols.containsKey(w2)) {
            return false;
        }

        Symbol s1 = symbols.get(w1);
        Symbol s2 = symbols.get(w2);

        if (s1.leftContext.containsKey(w2) || s1.rightContext.containsKey(w2)) {
            return true;
        }
        
        if (s2.leftContext.containsKey(w1) || s2.rightContext.containsKey(w1)) {
            return true;
        }
        
        return false;
    }

    private Map<Set<String>, Set<Structure>> groupPatternsByStructuralFamily(List<Pattern> allPatterns) {
        Map<Set<String>, Set<Structure>> families = new HashMap<Set<String>, Set<Structure>>();
        
        for (Pattern pattern : allPatterns) {
            if (pattern instanceof Structure) {
                Structure sp = (Structure) pattern;
                Set<String> structuralSignature = new HashSet<String>();
                for (String word : sp.getStructuralSlots()) {
                    if (!word.equals("[1]") && !word.equals("[2]")) {
                        structuralSignature.add(word);
                    }
                }
                
                if (!families.containsKey(structuralSignature)) {
                    families.put(structuralSignature, new HashSet<Structure>());
                }
                families.get(structuralSignature).add(sp);
            }
        }
        
        return families;
    }

    private Set<String> extractStructuralSignature(Set<List<String>> patternFamily) {
        Set<String> signature = new HashSet<String>();
        for (List<String> pattern : patternFamily) {
            for (String word : pattern) {
                if (!word.equals("[1]") && !word.equals("[2]")) {
                    signature.add(word);
                }
            }
        }
        return signature;
    }

    private boolean haveSimilarStructuralSignature(Set<String> sig1, Set<String> sig2) {
        if (sig1.isEmpty() || sig2.isEmpty()) return false;
        return sig1.size() == sig2.size();
    }

    private boolean samePatternStructure(List<String> p1, List<String> p2) {
        
        if (p1.size() != p2.size()) {
            return false;
        }
        
        for (int i = 0; i < p1.size(); i++) {
            String t1 = p1.get(i);
            String t2 = p2.get(i);
            
            boolean p1c = t1.equals("[1]") || t1.equals("[2]");
            boolean p2c = t2.equals("[1]") || t2.equals("[2]");
            
            if (p1c != p2c) {
                return false;
            }
            
            if (p1c) {
                if (!t1.equals(t2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean shareSemanticContext(Structure existingPattern, List<String> currentPattern, Set<String> currentContentWords) {
        
        Set<String> existingContentWords = new HashSet<String>();
        
        for(List<String> example : existingPattern.getConcreteExamples()) {
            for(String word : example) {
                if(symbols.containsKey(word) && 
                   (symbols.get(word).relations.contains("1") || symbols.get(word).relations.contains("2"))) {
                    existingContentWords.add(word);
                }
            }
        }
        
        for(String word : currentContentWords) {
            if(existingContentWords.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void findDifferingStructuralWords(List<String> p1, List<String> p2, Set<EquivalencePair> foundEquivalents) {
        
        for (int i = 0; i < p1.size(); i++) {
            String w1 = p1.get(i);
            String w2 = p2.get(i);
            
            boolean p1isStructural = !w1.equals("[1]") && !w1.equals("[2]");
            boolean p2isStructural = !w2.equals("[1]") && !w2.equals("[2]");
            
            if (p1isStructural && p2isStructural && !w1.equals(w2) && !areNeighbors(w1, w2)) {
                foundEquivalents.add(new EquivalencePair(w1, w2));
            }
        }
    }
}