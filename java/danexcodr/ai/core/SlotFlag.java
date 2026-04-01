package danexcodr.ai.core;

public class SlotFlag {
    public final boolean left;
    public final boolean right;
    
    public static final String S1 = "[1]";
    public static final String S2 = "[2]";
    public static final String SC = "[C]";
    public static final String SX = "[X]";
    
    // Factory constants
    public static final SlotFlag _1 = new SlotFlag(true, false);
    public static final SlotFlag _2 = new SlotFlag(false, true);
    public static final SlotFlag _C = new SlotFlag(true, true);
    public static final SlotFlag _X = new SlotFlag(false, false);
    
    private SlotFlag(boolean left, boolean right) {
        this.left = left;
        this.right = right;
    }
    
    // Convert to string token (for display)
    public String toToken() {
        if (left && !right) return S1;
        if (!left && right) return S2;
        if (left && right) return SC;
        return SX;
    }
    
    @Override
    public String toString() {
        return toToken();
    }
    
    // Helper method to check if a string is a slot flag
    public static boolean isFlagged(String token) {
        return S1.equals(token) || S2.equals(token) || SC.equals(token) || SX.equals(token);
    }
    
    // Helper method to check if a string is a term placeholder
    public static boolean isEither(String token) {
        return S1.equals(token) || S2.equals(token);
    }
    
    // Helper method to check if a string is a commutative placeholder
    public static boolean isBoth(String token) {
        return SC.equals(token);
    }
    
    // Helper method to check if a string is a single placeholder
    public static boolean isNeither(String token) {
        return SX.equals(token);
    }
}