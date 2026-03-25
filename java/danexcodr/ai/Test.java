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
                "",
                "v",
                "a",
                "cat",
                "canine",
                "a",
                "cat",
                "dog",
                "a",
                "cat",
                "mammal",
                "a",
                "cat",
                "animal",
                "q"
            };

    private static class EchoingInputStream extends InputStream {
        private final InputStream delegate;
        private final StringBuilder lineBuffer = new StringBuilder();
        private int lineNumber = 0;

        EchoingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws java.io.IOException {
            int value = delegate.read();
            if (value != -1) {
                processByte(value);
            } else {
                flushPendingLine();
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            int count = delegate.read(b, off, len);
            if (count > 0) {
                for (int i = off; i < off + count; i++) {
                    processByte(b[i] & 0xFF);
                }
            } else if (count == -1) {
                flushPendingLine();
            }
            return count;
        }

        @Override
        public void close() throws java.io.IOException {
            delegate.close();
        }

        private void processByte(int value) {
            if (value == '\r') {
                return;
            }
            if (value == '\n') {
                emitCurrentLine();
                return;
            }
            lineBuffer.append((char) value);
        }

        private void emitCurrentLine() {
            lineNumber++;
            String line = lineBuffer.toString();
            if (line.length() == 0) {
                line = "<empty>";
            }
            System.out.println("  [input " + lineNumber + "] " + line);
            lineBuffer.setLength(0);
        }

        private void flushPendingLine() {
            if (lineBuffer.length() > 0) {
                emitCurrentLine();
            }
        }
    }
    
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
            System.setIn(
                new EchoingInputStream(
                    new ByteArrayInputStream(inputSequence.getBytes(StandardCharsets.UTF_8))));
            
            // Run the main AI
            Main.main(args);
            
        } finally {
            // Restore original System.in
            System.setIn(originalIn);
        }
    }
}
