import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dependency-free deterministic tests for ReviewGate.
 * Run via test-review-gate.sh or Maven exec:exec@review-gate-tests.
 */
public class ReviewGateTest {

    private static int passed;

    public static void main(String[] args) throws Exception {
        run("clean response passes", ReviewGateTest::cleanResponsePasses);
        run("minor response passes", ReviewGateTest::minorResponsePasses);
        run("block response blocks", ReviewGateTest::blockResponseBlocks);
        run("missing severity fails closed", ReviewGateTest::missingSeverityFailsClosed);
        run("invalid severity fails closed", ReviewGateTest::invalidSeverityFailsClosed);
        run("duplicate severity fails closed", ReviewGateTest::duplicateSeverityFailsClosed);
        run("missing summary fails closed", ReviewGateTest::missingSummaryFailsClosed);
        run("client failure fails closed", ReviewGateTest::clientFailureFailsClosed);
        run("large diff is losslessly chunked", ReviewGateTest::largeDiffIsLosslesslyChunked);
        run("chunk severities aggregate to worst", ReviewGateTest::chunkSeveritiesAggregateToWorst);
        run("malformed later chunk fails entire gate", ReviewGateTest::malformedLaterChunkFailsEntireGate);
        run("Ollama response JSON is parsed strictly", ReviewGateTest::ollamaJsonIsParsedStrictly);
        run("missing Ollama response field fails closed", ReviewGateTest::missingOllamaResponseFailsClosed);

        System.out.println("ReviewGate tests passed: " + passed + "/13");
    }

    private static void cleanResponsePasses() throws Exception {
        ReviewGate.ReviewResult result = reviewWith("SEVERITY: NONE\nSUMMARY: clean");
        assertEquals(ReviewGate.Severity.NONE, result.severity());
    }

    private static void minorResponsePasses() throws Exception {
        ReviewGate.ReviewResult result = reviewWith("SEVERITY: MINOR\nSUMMARY: naming nit");
        assertEquals(ReviewGate.Severity.MINOR, result.severity());
    }

    private static void blockResponseBlocks() throws Exception {
        ReviewGate.ReviewResult result = reviewWith(
                "SEVERITY: BLOCK\nSUMMARY: swallowed exception\nISSUES:\n- Example.java exception ignored");
        assertEquals(ReviewGate.Severity.BLOCK, result.severity());
    }

    private static void missingSeverityFailsClosed() {
        expectGateFailure(() -> reviewWith("SUMMARY: looks fine"), "omitted SEVERITY");
    }

    private static void invalidSeverityFailsClosed() {
        expectGateFailure(() -> reviewWith("SEVERITY: HIGH\nSUMMARY: bad"), "invalid SEVERITY");
    }

    private static void duplicateSeverityFailsClosed() {
        expectGateFailure(() -> reviewWith(
                "SEVERITY: NONE\nSUMMARY: clean\nSEVERITY: BLOCK"), "more than one SEVERITY");
    }

    private static void missingSummaryFailsClosed() {
        expectGateFailure(() -> reviewWith("SEVERITY: NONE"), "omitted SUMMARY");
    }

    private static void clientFailureFailsClosed() {
        expectGateFailure(() -> ReviewGate.reviewDiff(
                smallDiff(),
                "test-model",
                (model, prompt) -> { throw new IOException("simulated timeout"); },
                24_000), "simulated timeout");
    }

    private static void largeDiffIsLosslesslyChunked() throws Exception {
        StringBuilder diff = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            diff.append("+line-").append(i).append(" abcdefghijklmnopqrstuvwxyz\n");
        }

        List<String> chunks = ReviewGate.splitDiff(diff.toString(), 200);
        assertTrue(chunks.size() > 1, "expected multiple chunks");
        assertEquals(diff.toString(), String.join("", chunks));
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 200, "chunk exceeded configured limit: " + chunk.length());
        }
    }

    private static void chunkSeveritiesAggregateToWorst() throws Exception {
        String diff = "+aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n"
                + "+bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\n"
                + "+cccccccccccccccccccccccccccccc\n";
        AtomicInteger call = new AtomicInteger();

        ReviewGate.ReviewResult result = ReviewGate.reviewDiff(
                diff,
                "test-model",
                (model, prompt) -> switch (call.incrementAndGet()) {
                    case 1 -> "SEVERITY: NONE\nSUMMARY: clean first chunk";
                    case 2 -> "SEVERITY: BLOCK\nSUMMARY: blocking second chunk";
                    default -> "SEVERITY: MINOR\nSUMMARY: minor later chunk";
                },
                40);

        assertTrue(result.chunksReviewed() >= 2, "expected more than one chunk");
        assertEquals(ReviewGate.Severity.BLOCK, result.severity());
        assertTrue(result.output().startsWith("SEVERITY: BLOCK"), "aggregate output must expose worst severity");
    }

    private static void malformedLaterChunkFailsEntireGate() {
        String diff = "+aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n"
                + "+bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\n";
        AtomicInteger call = new AtomicInteger();

        expectGateFailure(() -> ReviewGate.reviewDiff(
                diff,
                "test-model",
                (model, prompt) -> call.incrementAndGet() == 1
                        ? "SEVERITY: NONE\nSUMMARY: first chunk clean"
                        : "I forgot the required output format",
                35), "omitted SEVERITY");
    }

    private static void ollamaJsonIsParsedStrictly() throws Exception {
        String json = "{\"model\":\"x\",\"response\":\"SEVERITY: NONE\\nSUMMARY: clean \\u2713\",\"done\":true}";
        String parsed = ReviewGate.extractJsonStringFieldStrict(json, "response");
        assertEquals("SEVERITY: NONE\nSUMMARY: clean ✓", parsed);
    }

    private static void missingOllamaResponseFailsClosed() {
        expectGateFailure(() -> ReviewGate.extractJsonStringFieldStrict(
                "{\"model\":\"x\",\"done\":true}", "response"), "omitted field 'response'");
    }

    private static ReviewGate.ReviewResult reviewWith(String response) throws Exception {
        return ReviewGate.reviewDiff(
                smallDiff(),
                "test-model",
                (model, prompt) -> response,
                24_000);
    }

    private static String smallDiff() {
        return "diff --git a/Example.java b/Example.java\n"
                + "+++ b/Example.java\n"
                + "@@ -1 +1 @@\n"
                + "+class Example {}\n";
    }

    private static void run(String name, ThrowingRunnable test) throws Exception {
        try {
            test.run();
            passed++;
            System.out.println("PASS: " + name);
        } catch (Throwable t) {
            System.err.println("FAIL: " + name + " — " + t.getMessage());
            throw t;
        }
    }

    private static void expectGateFailure(ThrowingRunnable action, String expectedMessagePart) {
        try {
            action.run();
            throw new AssertionError("expected ReviewGateException");
        } catch (ReviewGate.ReviewGateException e) {
            assertTrue(e.getMessage().contains(expectedMessagePart),
                    "expected error containing '" + expectedMessagePart + "' but was '" + e.getMessage() + "'");
        } catch (Exception e) {
            throw new AssertionError("expected ReviewGateException but got " + e.getClass().getSimpleName(), e);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
