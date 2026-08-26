package io.mo.db;

import io.mo.cases.SqlCommand;
import io.mo.cases.TestScript;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Exercises capture failures through Executor.genRS, not just parser state. */
public class ExecutorCaptureTest {

    @Test
    public void captureQueryErrorAbortsResultGeneration() throws Exception {
        assertCaptureFailure(FailureMode.QUERY_ERROR);
    }

    @Test
    public void captureEmptyResultAbortsResultGeneration() throws Exception {
        assertCaptureFailure(FailureMode.EMPTY_RESULT);
    }

    @Test
    public void captureNullResultAbortsResultGeneration() throws Exception {
        assertCaptureFailure(FailureMode.NULL_VALUE);
    }

    private void assertCaptureFailure(FailureMode mode) throws Exception {
        File sql = File.createTempFile("capture-failure", ".sql");
        File result = new File(sql.getPath().replaceAll("\\.[A-Za-z]+", ".result"));
        try {
            TestScript script = new TestScript();
            script.setFileName(sql.getPath());
            SqlCommand command = new SqlCommand();
            command.append("select capture_value();\n");
            command.setCaptureName("value");
            script.addCommand(command);

            Tracker tracker = new Tracker();
            boolean generated = new Executor(new StubConnectionManager(mode, tracker)).genRS(script);

            assertFalse("a control-directive failure must not become expected output", generated);
            assertTrue("capture failure must not be written to the result file",
                    !result.exists() || result.length() == 0);
            assertTrue("capture statement must be closed", tracker.captureStatementClosed);
            assertTrue("test database must be dropped", tracker.testDatabaseDropped);
            if (mode != FailureMode.QUERY_ERROR) {
                assertTrue("capture result set must be closed", tracker.captureResultSetClosed);
            }
        } finally {
            sql.delete();
            result.delete();
        }
    }

    private enum FailureMode { QUERY_ERROR, EMPTY_RESULT, NULL_VALUE }

    private static final class Tracker {
        boolean captureStatementClosed;
        boolean captureResultSetClosed;
        boolean testDatabaseDropped;
    }

    private static final class StubConnectionManager extends ConnectionManager {
        private final Connection connection;

        StubConnectionManager(FailureMode mode, Tracker tracker) {
            connection = proxy(Connection.class, new ConnectionHandler(mode, tracker));
        }

        @Override public Connection getConnection() { return connection; }
        @Override public Connection getConnection(int index) { return connection; }
        @Override public Connection getConnection(int index, String user, String password) { return connection; }
        @Override public void reset() { }
    }

    private static final class ConnectionHandler implements InvocationHandler {
        private final FailureMode mode;
        private final Tracker tracker;
        ConnectionHandler(FailureMode mode, Tracker tracker) {
            this.mode = mode;
            this.tracker = tracker;
        }

        @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            String name = method.getName();
            if ("createStatement".equals(name)) return proxy(Statement.class, new StatementHandler(mode, tracker));
            if ("isClosed".equals(name)) return false;
            if ("isValid".equals(name)) return true;
            return defaultValue(method.getReturnType());
        }
    }

    private static final class StatementHandler implements InvocationHandler {
        private final FailureMode mode;
        private final Tracker tracker;
        private boolean captureStatement;
        StatementHandler(FailureMode mode, Tracker tracker) {
            this.mode = mode;
            this.tracker = tracker;
        }

        @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("execute".equals(name)) {
                String sql = (String) args[0];
                if (sql.contains("capture_value")) {
                    captureStatement = true;
                    if (mode == FailureMode.QUERY_ERROR) throw new SQLException("capture query failed");
                    return true;
                }
                if (sql.startsWith("drop database")) tracker.testDatabaseDropped = true;
                return false;
            }
            if ("getResultSet".equals(name)) {
                return proxy(ResultSet.class, new ResultSetHandler(mode, tracker));
            }
            if ("close".equals(name) && captureStatement) tracker.captureStatementClosed = true;
            return defaultValue(method.getReturnType());
        }
    }

    private static final class ResultSetHandler implements InvocationHandler {
        private final FailureMode mode;
        private final Tracker tracker;
        private boolean advanced;
        ResultSetHandler(FailureMode mode, Tracker tracker) {
            this.mode = mode;
            this.tracker = tracker;
        }

        @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            if ("next".equals(method.getName())) {
                if (mode == FailureMode.EMPTY_RESULT || advanced) return false;
                advanced = true;
                return true;
            }
            if ("getObject".equals(method.getName())) return null;
            if ("close".equals(method.getName())) tracker.captureResultSetClosed = true;
            return defaultValue(method.getReturnType());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == Boolean.TYPE) return false;
        if (type == Character.TYPE) return '\0';
        if (type == Byte.TYPE || type == Short.TYPE || type == Integer.TYPE || type == Long.TYPE) return 0;
        if (type == Float.TYPE) return 0F;
        if (type == Double.TYPE) return 0D;
        return null;
    }
}
