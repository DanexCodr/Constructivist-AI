package danexcodr.ai.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class SymbolManager {

    private Map<String, Symbol> symbols = new HashMap<String, Symbol>();
    private Map<String, String> aliasMap = new HashMap<String, String>(); // Maps raw -> canonical

    public Map<String, Symbol> getSymbols() {
        return symbols;
    }
    
    public String getCanonical(String token) {
        if (aliasMap.containsKey(token)) {
            // Recursive lookup to handle chains if any (though merge logic should flatten)
            return getCanonical(aliasMap.get(token));
        }
        return token;
    }

    public void ensureSymbolExists(String word) {
        String canonical = getCanonical(word);
        
        if (!symbols.containsKey(canonical)) {
            symbols.put(canonical, new Symbol(canonical));
        } else {
            Symbol symbol = symbols.get(canonical);
            symbol.frequency++;
            symbol.updateTimestamp(); 
        }
    }

    public void buildContext(List<String> sequence) {
        // First pass: ensure all symbols exist (using normalized forms)
        for (String word : sequence) {
            ensureSymbolExists(word);
        }
        
        // Second pass: build context using CANONICAL forms
        for (int i = 0; i < sequence.size(); i++) {
            String word = sequence.get(i);
            String canonicalWord = getCanonical(word);
            Symbol symbol = symbols.get(canonicalWord);
            
            if (symbol == null) continue; // Should not happen

            if (i > 0) {
                String leftWord = sequence.get(i - 1);
                String canonicalLeft = getCanonical(leftWord);
                symbol.leftContext.put(canonicalLeft, incrementCount(symbol.leftContext, canonicalLeft));
                symbol.updateTimestamp(); 
            }
            if (i < sequence.size() - 1) {
                String rightWord = sequence.get(i + 1);
                String canonicalRight = getCanonical(rightWord);
                symbol.rightContext.put(canonicalRight, incrementCount(symbol.rightContext, canonicalRight));
                symbol.updateTimestamp(); 
            }
        }
    }
    
    public void mergeSymbols(String aliasToken, String baseToken) {
        if (aliasToken.equals(baseToken)) return;
        
        Symbol aliasSym = symbols.get(aliasToken);
        Symbol baseSym = symbols.get(baseToken);
        
        if (aliasSym == null || baseSym == null) return;
        
        // Merge Frequencies
        baseSym.frequency += aliasSym.frequency;
        
        // Merge Contexts
        mergeContextMap(baseSym.leftContext, aliasSym.leftContext);
        mergeContextMap(baseSym.rightContext, aliasSym.rightContext);
        
        // Merge Relations
        baseSym.relations.addAll(aliasSym.relations);
        
        // Register Alias
        aliasMap.put(aliasToken, baseToken);
        
        // Remove old symbol
        symbols.remove(aliasToken);
        baseSym.updateTimestamp();
    }
    
    private void mergeContextMap(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String key = entry.getKey();
            Integer count = entry.getValue();
            Integer current = target.get(key);
            target.put(key, (current == null ? 0 : current) + count);
        }
    }

    private Integer incrementCount(Map<String, Integer> map, String key) {
        Integer count = map.get(key);
        return (count == null) ? 1 : count + 1;
    }
}