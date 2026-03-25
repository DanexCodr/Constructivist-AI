package danexcodr.ai;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

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
            if (b == null) {
                throw new NullPointerException("byte array must not be null");
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException(
                    "Invalid offset/length: off=" + off + ", len=" + len + ", arrayLength=" + b.length);
            }
            if (len == 0) {
                return 0;
            }

            int first = read();
            if (first == -1) {
                return -1;
            }
            b[off] = (byte) first;
            return 1;
        }

        @Override
        public void close() throws java.io.IOException {
            flushPendingLine();
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
            lineBuffer.write(value);
        }

        private void emitCurrentLine() {
            String line = new String(lineBuffer.toByteArray(), StandardCharsets.UTF_8);
            if (line.length() == 0) {
                // Make consumed blank input visible in the auto-run transcript.
                line = "<empty>";
            }
            System.out.println("  " + line);
            lineBuffer.reset();
        }

        private void flushPendingLine() {
            if (lineBuffer.size() > 0) {
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
