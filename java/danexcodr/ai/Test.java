package danexcodr.ai;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.PrintStream;

/**
 * Auto-runs the Main AI with the exact flow from the example.
 * This simulates user input by redirecting System.in.
 */
public class Test {

    private static final String[] BASE_LEARN_LINES = {
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
        "1 + 2 = 3",
        "2 + 1 = 3",
        "7 + 7 = 14",
        "14 = 7 + 7"
    };

    private static final String[] INPUT_LINES = {
        "l",
        BASE_LEARN_LINES[0],
        BASE_LEARN_LINES[1],
        BASE_LEARN_LINES[2],
        BASE_LEARN_LINES[3],
        BASE_LEARN_LINES[4],
        BASE_LEARN_LINES[5],
        BASE_LEARN_LINES[6],
        BASE_LEARN_LINES[7],
        BASE_LEARN_LINES[8],
        BASE_LEARN_LINES[9],
        BASE_LEARN_LINES[10],
        BASE_LEARN_LINES[11],
        BASE_LEARN_LINES[12],
        BASE_LEARN_LINES[13],
        BASE_LEARN_LINES[14],
        BASE_LEARN_LINES[15],
        BASE_LEARN_LINES[16],
        BASE_LEARN_LINES[17],
        BASE_LEARN_LINES[18],
        BASE_LEARN_LINES[19],
        BASE_LEARN_LINES[20],
        BASE_LEARN_LINES[21],
        BASE_LEARN_LINES[22],
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
        "p",
        "cat is a feline",
        "p",
        "unknown token chain",
        "x",
        "q"
    };

    private static final String[] REQUIRED_OUTPUT_MARKERS = {
        "Constructivist AI - Learn from equivalent sequences",
        Config.MAIN_COMMANDS_TEXT,
        "Discovered",
        "=== Pattern Families (",
        "=== Learned Structural Tokens ===",
        "Generated",
        "Matched relation pattern:",
        "No known patterns matched this sequence.",
        "Unknown command: 'x'",
        "Exiting Inferential AI."
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
            System.out.println(line);
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

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();

        try {

            StringBuilder inputBuilder = new StringBuilder();
            for (String line : INPUT_LINES) {
                inputBuilder.append(line).append("\n");
            }
            String inputSequence = inputBuilder.toString();

            System.out.println("Running with " + INPUT_LINES.length + " input lines...\n");

            PrintStream teeOut = new PrintStream(
                new TeeOutputStream(originalOut, capturedOutput),
                true,
                "UTF-8");
            System.setOut(teeOut);

            System.setIn(
                new EchoingInputStream(
                    new ByteArrayInputStream(inputSequence.getBytes(StandardCharsets.UTF_8))));

            Main.main(args);

            String output = new String(capturedOutput.toByteArray(), "UTF-8");
            verifyOutput(output);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    private static void verifyOutput(String output) {
        StringBuilder missing = new StringBuilder();
        int missingCount = 0;
        for (String marker : REQUIRED_OUTPUT_MARKERS) {
            if (output.indexOf(marker) < 0) {
                if (missingCount > 0) {
                    missing.append(", ");
                }
                missing.append("'").append(marker).append("'");
                missingCount++;
            }
        }
        if (missingCount > 0) {
            throw new AssertionError("Missing required output markers: " + missing.toString());
        }
    }

    private static class TeeOutputStream extends java.io.OutputStream {
        private final java.io.OutputStream first;
        private final java.io.OutputStream second;

        TeeOutputStream(java.io.OutputStream first, java.io.OutputStream second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            first.write(b);
            second.write(b);
        }

        @Override
        public void flush() throws java.io.IOException {
            first.flush();
            second.flush();
        }

        @Override
        public void close() throws java.io.IOException {
            flush();
        }
    }
}
