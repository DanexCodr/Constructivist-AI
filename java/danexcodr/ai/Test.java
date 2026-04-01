package danexcodr.ai;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private static final String[] INPUT_LINES = buildInputLines();

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

    private static String[] buildInputLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("l");
        Collections.addAll(lines, BASE_LEARN_LINES);
        Collections.addAll(
            lines,
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
            "g",
            "cat",
            "canine",
            "g",
            "cat",
            "dog",
            "g",
            "cat",
            "mammal",
            "g",
            "cat",
            "animal",
            "p",
            "cat is a feline",
            "p",
            "unknown token chain",
            "x",
            "q");
        return lines.toArray(new String[lines.size()]);
    }

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
            int count = delegate.read(b, off, len);
            if (count == -1) {
                flushPendingLine();
                return -1;
            }

            for (int i = off; i < off + count; i++) {
                processByte(b[i] & 0xFF);
            }
            return count;
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
            if (!output.contains(marker)) {
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
