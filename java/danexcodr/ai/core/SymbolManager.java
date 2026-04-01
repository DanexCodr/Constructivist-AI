package danexcodr.ai.core;

import danexcodr.ai.util.MapUtils;

import java.util.*;

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

    public void ensureSymbolExists(String token) {
        String canonical = getCanonical(token);
        
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
        for (String token : sequence) {
            ensureSymbolExists(token);
        }
        
        // Second pass: build context using CANONICAL forms
        for (int i = 0; i < sequence.size(); i++) {
            String token = sequence.get(i);
            String canonicalToken = getCanonical(token);
            Symbol symbol = symbols.get(canonicalToken);
            
            if (symbol == null) continue; // Should not happen

            if (i > 0) {
                String leftToken = sequence.get(i - 1);
                String canonicalLeft = getCanonical(leftToken);
                symbol.leftContext.put(canonicalLeft, MapUtils.increment(symbol.leftContext, canonicalLeft));
                symbol.updateTimestamp(); 
            }
            if (i < sequence.size() - 1) {
                String rightToken = sequence.get(i + 1);
                String canonicalRight = getCanonical(rightToken);
                symbol.rightContext.put(canonicalRight, MapUtils.increment(symbol.rightContext, canonicalRight));
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
        MapUtils.merge(baseSym.leftContext, aliasSym.leftContext);
        MapUtils.merge(baseSym.rightContext, aliasSym.rightContext);
        
        // Merge Relations
        baseSym.relations.addAll(aliasSym.relations);
        
        // Register Alias
        aliasMap.put(aliasToken, baseToken);
        
        // Remove old symbol
        symbols.remove(aliasToken);
        baseSym.updateTimestamp();
    }
}