package ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Minimal runner: no JUnit, runs with plain javac/java. */
public final class TestRunner {

    public interface Body {
        void run() throws Exception;
    }

    private record Case(String name, Body body) {
    }

    private final List<Case> cases = new ArrayList<>();

    public static TestRunner suite() {
        return new TestRunner();
    }

    public TestRunner test(String name, Body body) {
        cases.add(new Case(name, body));
        return this;
    }

    public TestRunner add(TestRunner other) {
        cases.addAll(other.cases);
        return this;
    }

    /** Runs every case; returns 0 when all pass. */
    public int run() {
        int passed = 0;
        int failed = 0;
        System.out.println("running " + cases.size() + " tests");
        for (Case testCase : cases) {
            try {
                testCase.body().run();
                passed++;
                System.out.println("  PASS  " + testCase.name());
            } catch (Throwable failure) {
                failed++;
                System.out.println("  FAIL  " + testCase.name());
                System.out.println("        " + failure);
            }
        }
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        return failed == 0 ? 0 : 1;
    }

    public static void assertEquals(String what, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertTrue(String what, boolean condition) {
        if (!condition) {
            throw new AssertionError(what);
        }
    }

    public static void assertThrows(String what, Class<? extends Throwable> type, Runnable body) {
        try {
            body.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) {
                return;
            }
            throw new AssertionError(what + ": expected " + type.getSimpleName()
                    + " but got " + thrown.getClass().getSimpleName());
        }
        throw new AssertionError(what + ": expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
