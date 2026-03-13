package danexcodr.ai;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Auto-runs the Main AI with the exact flow from the example.
 * This simulates user input by redirecting System.in.
 */
public class Test {
    
                // Input sequence as array (cleaner to read and maintain)
            static String[] inputLines = {
                "l",                    // Learn command
                "cat is a feline",
                "cat is feline",
                "dog is a canine",
                "dog is canine",
                "canine is a mammal",
                "canine is mammal",
                "feline is a mammal",
                "feline is mammal",
                "canine and feline",
                "feline and canine",
                "canine is not a feline",
                "canine is not feline",
                "feline is not canine",
                "feline is not a canine",
                "mammal is an animal",
                "mammal is animal",
                "if hungry eat",
                "eat if hungry",
                "if hungry then eat",
                "",                     // Empty line to finish learning
                "v",                    // View command
                "g",                    // Generate command
                "cat",                  // term1
                "canine",               // term2
                "g",                    // Generate command
                "cat",                  // term1
                "dog",                  // term2
                "g",                    // Generate command
                "cat",                  // term1
                "mammal",               // term2
                "g",                    // Generate command
                "cat",                  // term1
                "animal",               // term2
                "gr",                   // Grammar command
                "cat is a animal",      // should become "cat is an animal"
                "gr",                   // Grammar command
                "cat is an mammal",     // should become "cat is a mammal"
                "gr",                   // Grammar command
                "mammal is an animal",  // already correct, unchanged
                "q"                     // Quit command
            };
    
    public static void main(String[] args) throws Exception {
        System.out.println("--- Starting Constructivist AI Auto-Run ---\n");
        
        // Save the original System.in
        InputStream originalIn = System.in;
        
        try {
            
            // Join array with newlines
            StringBuilder inputBuilder = new StringBuilder();
            for (String line : inputLines) {
                inputBuilder.append(line).append("\n");
            }
            String inputSequence = inputBuilder.toString();
            
            System.out.println("Running with " + inputLines.length + " input lines...\n");
            
            // Redirect System.in to our input sequence
            System.setIn(new ByteArrayInputStream(inputSequence.getBytes(StandardCharsets.UTF_8)));
            
            // Run the main AI
            Main.main(args);
            
        } finally {
            // Restore original System.in
            System.setIn(originalIn);
        }
    }
}