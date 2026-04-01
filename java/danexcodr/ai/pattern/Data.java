package danexcodr.ai.pattern;

import danexcodr.ai.core.SlotFlag;
import java.util.*;

public class Data {
    private List<String> tokens;
    private List<SlotFlag> placeholders;
    private List<String> pfTokens;
    private List<String> aliases;
    private List<Integer> order;
    
    public Data() {
        this.tokens = new ArrayList<>();
        this.placeholders = new ArrayList<>();
        this.pfTokens = new ArrayList<>();
        this.aliases = new ArrayList<>();
        this.order = new ArrayList<>();
    }
    
    public void addToken(String token) {
        if (token == null) throw new IllegalArgumentException("Token cannot be null");
        tokens.add(token);
        order.add(0);
    }
    
    public void addPlaceholder(SlotFlag flag) {
        if (flag == null) throw new IllegalArgumentException("Placeholder cannot be null");
        placeholders.add(flag);
        order.add(1);
    }
    
    public void addPFToken(String pfToken) {
        if (pfToken == null) throw new IllegalArgumentException("PF token cannot be null");
        if (!pfToken.startsWith("PF")) {
            throw new IllegalArgumentException("PF token must start with 'PF': " + pfToken);
        }
        pfTokens.add(pfToken);
        order.add(2);
    }
    
    public void addAlias(String alias) {
        if (alias == null) throw new IllegalArgumentException("Alias cannot be null");
        if (!alias.startsWith("[S")) {
            throw new IllegalArgumentException("Alias must start with '[S': " + alias);
        }
        aliases.add(alias);
        order.add(3);
    }
    
    public int size() {
        return order.size();
    }
    
    public boolean isPlaceholder(int index) {
        if (index < 0 || index >= order.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + order.size());
        }
        return order.get(index) == 1;
    }
    
    public boolean isToken(int index) {
        if (index < 0 || index >= order.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + order.size());
        }
        return order.get(index) == 0;
    }
    
    public boolean isPFToken(int index) {
        if (index < 0 || index >= order.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + order.size());
        }
        return order.get(index) == 2;
    }
    
    public boolean isAlias(int index) {
        if (index < 0 || index >= order.size()) {
            throw new IndexOutOfBoundsException(
                "Index: " + index + ", Size: " + order.size());
        }
        return order.get(index) == 3;
    }
    
    public SlotFlag getPlaceholderAt(int index) {
        if (!isPlaceholder(index)) {
            throw new IllegalStateException(
                "Token at index " + index + " is not a placeholder");
        }
        int count = 0;
        for (int i = 0; i <= index; i++) {
            if (order.get(i) == 1) count++;
        }
        return placeholders.get(count - 1);
    }
    
    public String getTokenAt(int index) {
        if (!isToken(index)) {
            throw new IllegalStateException(
                "Token at index " + index + " is not a token");
        }
        int count = 0;
        for (int i = 0; i <= index; i++) {
            if (order.get(i) == 0) count++;
        }
        return tokens.get(count - 1);
    }
    
    public String getPFTokenAt(int index) {
        if (!isPFToken(index)) {
            throw new IllegalStateException(
                "Token at index " + index + " is not a PF token");
        }
        int count = 0;
        for (int i = 0; i <= index; i++) {
            if (order.get(i) == 2) count++;
        }
        return pfTokens.get(count - 1);
    }
    
    public String getAliasAt(int index) {
        if (!isAlias(index)) {
            throw new IllegalStateException(
                "Token at index " + index + " is not an alias");
        }
        int count = 0;
        for (int i = 0; i <= index; i++) {
            if (order.get(i) == 3) count++;
        }
        return aliases.get(count - 1);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Data other = (Data) obj;
        
        if (size() != other.size()) return false;
        
        for (int i = 0; i < size(); i++) {
            if (isPlaceholder(i) != other.isPlaceholder(i)) return false;
            if (isToken(i) != other.isToken(i)) return false;
            if (isPFToken(i) != other.isPFToken(i)) return false;
            if (isAlias(i) != other.isAlias(i)) return false;
            
            if (isPlaceholder(i) && getPlaceholderAt(i) != other.getPlaceholderAt(i)) return false;
            if (isToken(i) && !getTokenAt(i).equals(other.getTokenAt(i))) return false;
            if (isPFToken(i) && !getPFTokenAt(i).equals(other.getPFTokenAt(i))) return false;
            if (isAlias(i) && !getAliasAt(i).equals(other.getAliasAt(i))) return false;
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        for (int i = 0; i < size(); i++) {
            if (isPlaceholder(i)) {
                result = 31 * result + getPlaceholderAt(i).hashCode();
            } else if (isToken(i)) {
                result = 31 * result + getTokenAt(i).hashCode();
            } else if (isPFToken(i)) {
                result = 31 * result + getPFTokenAt(i).hashCode();
            } else if (isAlias(i)) {
                result = 31 * result + getAliasAt(i).hashCode();
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < size(); i++) {
            if (i > 0) sb.append(", ");
            if (isPlaceholder(i)) {
                sb.append(getPlaceholderAt(i).toToken());
            } else if (isToken(i)) {
                sb.append(getTokenAt(i));
            } else if (isPFToken(i)) {
                sb.append(getPFTokenAt(i));
            } else if (isAlias(i)) {
                sb.append(getAliasAt(i));
            }
        }
        sb.append("]");
        return sb.toString();
    }
}