package de.unistuttgart.stayinsync.scriptengine;

import de.unistuttgart.stayinsync.exception.ScriptEngineException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ContextPoolTests.ContextPoolTestProfile.class)
public class ContextPoolTests {

    private static final String TEST_LANG_ID = "js";
    private static final int DEFAULT_POOL_SIZE = 3;
    private static final int BORROW_TIMEOUT_SECONDS = 10;

    private ContextPool contextPool;

    private static CapturingLogHandler capturingLogHandler;
    private static Logger contextPoolLoggerJul;

    public static class ContextPoolTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "org.jboss.logging.provider", "jdk",
                    "quarkus.log.category.\"de.unistuttgart.stayinsync.scriptengine.ContextPool\".level", "ALL",
                    "quarkus.log.category.\"de.unistuttgart.stayinsync.scriptengine.ContextPool\".min-level", "ALL",
                    "quarkus.log.console.enable", "false"
            );
        }
    }

    public static class CapturingLogHandler extends Handler {
        private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());
        private final String targetLoggerNamePrefix;

        public CapturingLogHandler(String targetLoggerNamePrefix) {
            this.targetLoggerNamePrefix = targetLoggerNamePrefix;
        }

        @Override
        public void publish(LogRecord record) {
            if (record.getLoggerName().startsWith(targetLoggerNamePrefix) ||
                    record.getLoggerName().equals(Logger.getLogger(String.valueOf(ContextPool.class)).getName())) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        public List<LogRecord> getRecords() {
            return new ArrayList<>(records);
        }

        public void clearRecords() {
            records.clear();
        }
    }


    @BeforeEach
    void setUp() {
        capturingLogHandler = new CapturingLogHandler(ContextPool.class.getPackageName());
        capturingLogHandler.setLevel(Level.ALL);

        contextPoolLoggerJul = Logger.getLogger(ContextPool.class.getName());
        for (Handler h : contextPoolLoggerJul.getHandlers()) contextPoolLoggerJul.removeHandler(h);
        contextPoolLoggerJul.addHandler(capturingLogHandler);
        contextPoolLoggerJul.setLevel(Level.ALL);
        contextPoolLoggerJul.setUseParentHandlers(false);
    }

    @AfterEach
    void tearDown() {
        if (contextPool != null) {
            contextPool.closeAllContexts();
        }
        if (contextPoolLoggerJul != null && capturingLogHandler != null) {
            contextPoolLoggerJul.removeHandler(capturingLogHandler);
        }
        assert capturingLogHandler != null;
        capturingLogHandler.clearRecords();
    }

    private List<LogRecord> getLogsByName(String loggerName) {
        return capturingLogHandler.getRecords().stream()
                .filter(r -> r.getLoggerName().equals(loggerName))
                .collect(Collectors.toList());
    }

    private List<LogRecord> getContextPoolLogs() {
        return getLogsByName(ContextPool.class.getName());
    }


    @Test
    void constructor_shouldInitializePoolWithCorrectSizeAndLanguageId() throws ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, DEFAULT_POOL_SIZE);
        assertEquals(DEFAULT_POOL_SIZE, contextPool.getPoolSize());
        assertEquals(DEFAULT_POOL_SIZE, contextPool.getAvailableCount(), "All contexts should be available after init.");
        assertEquals(TEST_LANG_ID, contextPool.getLanguageId());

        List<Context> borrowedContexts = new ArrayList<>();
        try {
            for (int i = 0; i < DEFAULT_POOL_SIZE; i++) {
                Context ctx = contextPool.borrowContext();
                assertNotNull(ctx, "Borrowed context should not be null");
                assertTrue(ctx.getEngine().getLanguages().containsKey("js"), "Context should support JS");
                borrowedContexts.add(ctx);
            }
        } catch (InterruptedException e) {
            fail("Should not be interrupted during initial borrow", e);
        } finally {
            borrowedContexts.forEach(contextPool::returnContext);
        }
    }

    @Test
    void constructor_withZeroSize_shouldThrowIllegalArgumentException() {
        ScriptEngineException exception = assertThrows(ScriptEngineException.class, () -> new ContextPool(TEST_LANG_ID, 0));
        assertTrue(exception.getMessage().contains("ContextPool size must be positive"),
                "Exception message mismatch for zero size.");
        assertTrue(exception.getMessage().contains("0"), "Exception message should contain the invalid size.");
    }

    @Test
    void constructor_withNegativeSize_shouldThrowIllegalArgumentException() {
        ScriptEngineException exception = assertThrows(ScriptEngineException.class, () -> new ContextPool(TEST_LANG_ID, -5));
        assertTrue(exception.getMessage().contains("ContextPool size must be positive"),
                "Exception message mismatch for negative size.");
        assertTrue(exception.getMessage().contains("-5"), "Exception message should contain the invalid size.");
    }

    @Test
    void borrowContext_whenAvailable_shouldReturnContextAndDecrementCount() throws InterruptedException, ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, 1);
        assertEquals(1, contextPool.getAvailableCount());

        Context context = contextPool.borrowContext();
        assertNotNull(context);
        assertEquals(0, contextPool.getAvailableCount());

        contextPool.returnContext(context);
    }

    @Test
    @Timeout(value = BORROW_TIMEOUT_SECONDS + 2)
    void borrowContext_whenPoolEmpty_shouldBlockAndTimeout() throws InterruptedException, ScriptEngineException  {
        contextPool = new ContextPool(TEST_LANG_ID, 1);
        Context firstContext = contextPool.borrowContext();
        assertNotNull(firstContext);
        assertEquals(0, contextPool.getAvailableCount());

        long startTime = System.currentTimeMillis();
        ScriptEngineException exception = assertThrows(ScriptEngineException.class, () -> contextPool.borrowContext(), "Should throw ScriptEngineException on timeout");
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(exception.getMessage().contains("Timeout borrowing context"), "Exception message mismatch");
        assertTrue(duration >= TimeUnit.SECONDS.toMillis(BORROW_TIMEOUT_SECONDS),
                "Borrow should have blocked for at least " + BORROW_TIMEOUT_SECONDS + "s, but was " + duration + "ms");

        assertTrue(getContextPoolLogs().stream()
                        .anyMatch(r -> r.getLevel().equals(Level.WARNING) &&
                                r.getMessage().contains("Timeout borrowing context for language")),
                "Expected timeout warning log not found.");

        contextPool.returnContext(firstContext);
    }

    @Test
    void borrowContext_whenInterruptedWhileWaiting_shouldThrowInterruptedException() throws InterruptedException, ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, 1);
        Context first = contextPool.borrowContext();

        AtomicReference<Throwable> exceptionInThread = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                contextPool.borrowContext();
            } catch (InterruptedException | ScriptEngineException e) {
                exceptionInThread.set(e);
            }
        });

        t.start();
        Thread.sleep(100);
        assertTrue(t.isAlive(), "Borrowing thread should be alive and blocking");
        t.interrupt();
        t.join(1000);

        assertFalse(t.isAlive(), "Borrowing thread should have terminated");
        assertNotNull(exceptionInThread.get(), "InterruptedException should have been thrown");
        assertInstanceOf(InterruptedException.class, exceptionInThread.get(), "Exception should be InterruptedException");

        contextPool.returnContext(first);
    }


    @Test
    void returnContext_whenPoolNotFull_shouldReturnTrueAndIncrementCount() throws InterruptedException, ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, 1);
        Context context = contextPool.borrowContext();
        assertEquals(0, contextPool.getAvailableCount());

        boolean returned = contextPool.returnContext(context);
        assertTrue(returned);
        assertEquals(1, contextPool.getAvailableCount());
    }

    @Test
    void returnContext_withNullContext_shouldReturnFalseAndNotChangeCount() throws ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, 1);
        int initialCount = contextPool.getAvailableCount();

        boolean returned = contextPool.returnContext(null);
        assertFalse(returned);
        assertEquals(initialCount, contextPool.getAvailableCount());
        assertTrue(getContextPoolLogs().stream().noneMatch(r -> r.getMessage().contains("null context")),
                "No log expected for returning null context.");
    }

    @Test
    void returnContext_whenExternalContextOffered_shouldBeRejectedAndClosed() throws ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, 1);
        Context extraContext = Context.create("js");

        assertEquals(1, contextPool.getAvailableCount());

        boolean returned = contextPool.returnContext(extraContext);
        assertFalse(returned, "Should return false when rejecting an external context.");
        assertEquals(1, contextPool.getAvailableCount(), "Available count should not change.");

        try {
            extraContext.eval("js", "1+1");
            fail("Extra context should have been closed.");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"),
                    "Exception message indicates context closed. Actual: " + e.getMessage());
        }

        String expectedWarningSubstring = "Attempt to return a context to pool";
        String expectedWarningSubstring2 = "that did not originate from it";
        assertTrue(getContextPoolLogs().stream()
                        .anyMatch(r -> r.getLevel().equals(Level.WARNING) &&
                                r.getMessage().contains(expectedWarningSubstring) &&
                                r.getMessage().contains(expectedWarningSubstring2)),
                "Expected 'did not originate from pool' warning log not found. Captured logs: " +
                        getContextPoolLogs().stream()
                                .filter(r -> r.getLevel().equals(Level.WARNING))
                                .map(LogRecord::getMessage).toList());
    }


    @Test
    void closeAllContexts_shouldCloseAllContextsAndEmptyPool() throws InterruptedException, ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, DEFAULT_POOL_SIZE);
        Context borrowed = contextPool.borrowContext();
        assertEquals(DEFAULT_POOL_SIZE - 1, contextPool.getAvailableCount());

        contextPool.closeAllContexts();

        assertEquals(0, contextPool.getAvailableCount(), "Pool should be empty after closeAllContexts");

        try {
            borrowed.eval("js", "1+1");
            fail("Borrowed context should have been closed by closeAllContexts");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().toLowerCase().contains("closed"));
        }

        assertThrows(ScriptEngineException.class, () -> contextPool.borrowContext(),
                "Borrowing from a closed pool should throw IllegalStateException.");

        assertTrue(getContextPoolLogs().stream()
                        .anyMatch(r -> r.getLevel().equals(Level.INFO) &&
                                r.getMessage().contains("Closing all contexts in pool")),
                "Expected 'Closing all contexts' info log not found.");
    }

    @Test
    void concurrentBorrowAndReturn_shouldMaintainPoolIntegrity() throws InterruptedException, ScriptEngineException {
        int poolSize = 5;
        int numThreads = 10;
        int operationsPerThread = 20;
        contextPool = new ContextPool(TEST_LANG_ID, poolSize);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads * operationsPerThread);

        AtomicBoolean testFailed = new AtomicBoolean(false);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    Context context = null;
                    try {
                        context = contextPool.borrowContext();
                        assertNotNull(context, "Borrowed context should not be null in concurrent test");
                        Thread.sleep((long) (Math.random() * 10));
                        context.eval("js", "Math.random()");
                    } catch (InterruptedException e) {
                        System.err.println("Thread interrupted during borrow/return: " + e.getMessage());
                        testFailed.set(true);
                        Thread.currentThread().interrupt();
                        break;
                    } catch (PolyglotException e) {
                        System.err.println("PolyglotException during concurrent work: " + e.getMessage());
                        testFailed.set(true);
                        break;
                    } catch (Exception e) {
                        System.err.println("Unexpected exception during concurrent work: " + e.getMessage());
                        e.printStackTrace();
                        testFailed.set(true);
                        break;
                    } finally {
                        if (context != null) {
                            boolean returned = contextPool.returnContext(context);
                            if (!returned && contextPool.getAvailableCount() < contextPool.getPoolSize()) {
                                System.err.println("Failed to return context when pool wasn't full.");
                                testFailed.set(true);
                            }
                        }
                        latch.countDown();
                    }
                    if (testFailed.get()) break;
                }
            });
        }

        assertTrue(latch.await(BORROW_TIMEOUT_SECONDS * operationsPerThread / 2 + 5, TimeUnit.SECONDS),
                "All operations did not complete in time.");
        executor.shutdownNow();

        assertFalse(testFailed.get(), "Concurrent test encountered a failure.");
        assertEquals(poolSize, contextPool.getAvailableCount(), "All contexts should be returned to the pool after concurrent operations.");
    }

    // Note: Testing initializePool's error handling for Context.newBuilder().build() throwing an exception
    // is hard without mocking static methods or using PowerMock/equivalent, or a custom ContextFactory.
    // We can, however, observe that if such an error "were" to occur (and be logged),
    // the available count might be less than poolSize.
    // This is implicitly covered if we were to reduce pool size and see fewer contexts.

    @Test
    void logging_debugMessagesForBorrowAndReturn() throws InterruptedException, ScriptEngineException {
        contextPool = new ContextPool(TEST_LANG_ID, 1);
        capturingLogHandler.clearRecords();

        Context ctx = contextPool.borrowContext();
        assertTrue(getContextPoolLogs().stream()
                        .anyMatch(r -> r.getLevel().equals(Level.FINE) &&
                                r.getMessage().startsWith("Borrowed context") &&
                                r.getMessage().contains("Available: 0/1")),
                "Missing 'Borrowed context' debug log or incorrect content.");


        capturingLogHandler.clearRecords();
        contextPool.returnContext(ctx);
        assertTrue(getContextPoolLogs().stream()
                        .anyMatch(r -> r.getLevel().equals(Level.FINE) &&
                                r.getMessage().startsWith("Returned context") &&
                                r.getMessage().contains("Available: 1/1")),
                "Missing 'Returned context' debug log or incorrect content.");
    }
}