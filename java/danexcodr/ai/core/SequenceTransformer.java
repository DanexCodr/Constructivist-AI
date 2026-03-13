package danexcodr.ai.core;

import java.util.*;

public class SequenceTransformer {
    
    public static List<String> abstractSequence(List<String> sequence, String term1, String term2) {
        List<String> abstracted = new ArrayList<String>();
        for (String word : sequence) {
            if (word.equals(term1)) {
                abstracted.add("[1]");
            } else if (word.equals(term2)) {
                abstracted.add("[2]");
            } else {
                abstracted.add(word);
            }
        }
        return abstracted;
    }
    
    public static List<String> abstractSequencePF(List<String> sequence, String term1, String term2) {
        List<String> abstracted = new ArrayList<String>();
        for (String word : sequence) {
            if (word.equals(term1)) {
                abstracted.add(word.startsWith("PF") ? word : "[1]");
            } else if (word.equals(term2)) {
                abstracted.add(word.startsWith("PF") ? word : "[2]");
            } else {
                abstracted.add(word);
            }
        }
        return abstracted;
    }
    
    public static List<String> flipTermPattern(List<String> pattern) {
        List<String> flipped = new ArrayList<String>();
        for (String token : pattern) {
            if (token.equals("[1]")) {
                flipped.add("[2]");
            } else if (token.equals("[2]")) {
                flipped.add("[1]");
            } else {
                flipped.add(token);
            }
        }
        return flipped;
    }
    
    public static List<String> flipTermPatternWithTerms(List<String> pattern, String term1, String term2) {
        List<String> flipped = new ArrayList<String>();
        for (String token : pattern) {
            if (token.equals("[1]")) {
                flipped.add("[2]");
            } else if (token.equals("[2]")) {
                flipped.add(term1.startsWith("PF") ? term1 : "[1]");
            } else if (token.equals(term1)) {
                flipped.add(term2);
            } else if (token.equals(term2)) {
                flipped.add(term1);
            } else {
                flipped.add(token);
            }
        }
        return flipped;
    }
    
    // NEW: Convert PF tokens to placeholders
    public static List<String> convertPFToPlaceholders(List<String> sequence, Map<String, String> pfToPlaceholder) {
        List<String> converted = new ArrayList<String>();
        for (String token : sequence) {
            if (token.startsWith("PF") && pfToPlaceholder.containsKey(token)) {
                converted.add(pfToPlaceholder.get(token));
            } else {
                converted.add(token);
            }
        }
        return converted;
    }
    
    // NEW: Extract PF tokens from sequence
    public static List<String> extractPFTokens(List<String> sequence) {
        List<String> pfTokens = new ArrayList<String>();
        for (String token : sequence) {
            if (token.startsWith("PF")) {
                pfTokens.add(token);
            }
        }
        return pfTokens;
    }
}