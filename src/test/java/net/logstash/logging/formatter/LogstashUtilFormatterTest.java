/*
 * Copyright 2013 karl spies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logging.formatter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ListResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LogstashUtilFormatterTest {

    private static final String EXPECTED_EX_STACKTRACE = "java.lang.Exception: That is an exception\n"
            + "\tat Test.methodTest(Test.class:42)\n"
            + "Caused by: java.lang.Exception: This is the cause\n"
            + "\tat Cause.methodCause(Cause.class:69)\n";

    private LogRecord record = null;
    private LogstashUtilFormatter instance = new LogstashUtilFormatter();
    private String fullLogMessage = null;
    private JsonObjectBuilder fieldsBuilder;
    private JsonObjectBuilder builder;
    private Exception ex, cause;
    private static String hostName;

    static {
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown-host";
        }
    }

    public static Exception buildException(final String message, final Throwable cause,
            final StackTraceElement...stackTrace) {
        final Exception result = new Exception(message, cause);
        result.setStackTrace(stackTrace);
        return result;
    }

    /**
     *
     */
    @Before
    public void setUp() {
        cause = buildException("This is the cause", null,
                new StackTraceElement("Cause", "methodCause", "Cause.class", 69));

        ex = buildException("That is an exception", cause,
                new StackTraceElement("Test", "methodTest", "Test.class", 42));

        long millis = System.currentTimeMillis();
        record = new LogRecord(Level.ALL, "Junit Test");
        record.setLoggerName(LogstashUtilFormatter.class.getName());
        record.setSourceClassName(LogstashUtilFormatter.class.getName());
        record.setSourceMethodName("testMethod");
        record.setMillis(millis);
        record.setThrown(ex);

        builder = Json.createBuilderFactory(null).createObjectBuilder();
        final SimpleDateFormat dateFormat = new SimpleDateFormat(LogstashUtilFormatter.DATE_FORMAT);
        String dateString = dateFormat.format(new Date(millis));
        builder.add("@timestamp", dateString);
        builder.add("@message", "Junit Test");
        builder.add("@source", LogstashUtilFormatter.class.getName());
        builder.add("@source_host", hostName);

        fieldsBuilder = Json.createBuilderFactory(null).createObjectBuilder();
        fieldsBuilder.add("timestamp", millis);
        fieldsBuilder.add("level", Level.ALL.toString());
        fieldsBuilder.add("line_number", ex.getStackTrace()[0].getLineNumber());
        fieldsBuilder.add("class", LogstashUtilFormatter.class.getName());
        fieldsBuilder.add("method", "testMethod");
        fieldsBuilder.add("exception_class", ex.getClass().getName());
        fieldsBuilder.add("exception_message", ex.getMessage());
        fieldsBuilder.add("stacktrace", EXPECTED_EX_STACKTRACE);

        builder.add("@fields", fieldsBuilder);

        JsonArrayBuilder tagsBuilder = Json.createArrayBuilder();
        tagsBuilder.add("foo");
        tagsBuilder.add("bar");
        builder.add("@tags", tagsBuilder.build());

        fullLogMessage = builder.build().toString() + "\n";
    }

    /**
     * Test of format method, of class LogstashFormatter.
     */
    @Test
    public void testFormat() {
        String result = instance.format(record);
        assertEquals(fullLogMessage, result);
    }

    /**
     * Test of encodeFields method, of class LogstashFormatter.
     */
    @Test
    public void testEncodeFields() {
        JsonObjectBuilder result = instance.encodeFields(record);
        assertEquals(fieldsBuilder.build().toString(), result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfo() {
        final String expected = Json.createBuilderFactory(null).createObjectBuilder()
            .add("exception_class", ex.getClass().getName())
            .add("exception_message", ex.getMessage())
            .add("stacktrace", EXPECTED_EX_STACKTRACE)
            .build().toString();

        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        instance.addThrowableInfo(record, result);
        assertEquals(expected, result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoNoThrowableAttached() {
        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        instance.addThrowableInfo(new LogRecord(Level.OFF, hostName), result);
        assertEquals("{}", result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoThrowableAttachedButWithoutSourceClassName() {
        final String expected = Json.createBuilderFactory(null).createObjectBuilder()
                .add("exception_message", ex.getMessage())
                .add("stacktrace", EXPECTED_EX_STACKTRACE)
                .build().toString();

        record.setSourceClassName(null);

        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        instance.addThrowableInfo(record, result);
        assertEquals(expected, result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoThrowableAttachedButWithoutMessage() {
        final Exception ex2 = buildException(null, null, new StackTraceElement[0]);
        record.setThrown(ex2);

        final String expected = Json.createBuilderFactory(null).createObjectBuilder()
                .add("exception_class", ex2.getClass().getName())
                .add("stacktrace", "java.lang.Exception\n")
                .build().toString();

        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        instance.addThrowableInfo(record, result);
        assertEquals(expected, result.build().toString());
    }

    /**
     * Test of getLineNumber method, of class LogstashFormatter.
     */
    @Test
    public void testGetLineNumber() {
        int result = instance.getLineNumber(record);
        assertEquals(ex.getStackTrace()[0].getLineNumber(), result);
    }

    /**
     * Test of getLineNumber method, of class LogstashFormatter.
     */
    @Test
    public void testGetLineNumberNoThrown() {
        assertEquals(0, instance.getLineNumber(new LogRecord(Level.OFF, "foo")));
    }

    /**
     * Test of getLineNumberFromStackTrace method, of class LogstashUtilFormatter.
     */
    @Test
    public void testGetLineNumberFromStackTrace() {
        assertEquals(0, instance.getLineNumberFromStackTrace(new StackTraceElement[0]));
        assertEquals(0, instance.getLineNumberFromStackTrace(new StackTraceElement[]{null}));
    }

    /**
     * Test of addValue method, of class LogstashUtilFormatter.
     */
    @Test
    public void testAddValue() {
        JsonObjectBuilder builder = Json.createBuilderFactory(null).createObjectBuilder();
        instance.addValue(builder, "key", "value");
        assertEquals("{\"key\":\"value\"}", builder.build().toString());
    }

    /**
     * Test of addValue method, of class LogstashUtilFormatter.
     */
    @Test
    public void testAddNullValue() {
        JsonObjectBuilder builder = Json.createBuilderFactory(null).createObjectBuilder();
        instance.addValue(builder, "key", null);
        assertEquals("{\"key\":\"null\"}", builder.build().toString());
    }

    @Test
    public void testFormatMessageWithSquigglyFormat() {
        record.setMessage("{0} %s");
        record.setParameters(new Object[] { "hi" });
        assertEquals("hi %s", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithSquigglyFormatAndNullParameters() {
        record.setMessage("{0}");
        record.setParameters(null);
        assertEquals("{0}", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithSquigglyFormatAndEmptyParameters() {
        record.setMessage("{0}");
        record.setParameters(new Object[0]);
        assertEquals("{0}", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithBogusSquigglyFormatAndOkPercentFormat() {
        // this will fail the squiggly formatting, and fall back to % formatting
        record.setMessage("{0'}' %s");
        record.setParameters(new Object[] { "hi" });
        assertEquals("{0'}' hi", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithPercentFormat() {
        record.setMessage("%s");
        record.setParameters(new Object[] { "hi" });
        assertEquals("hi", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithPercentFormatAndNullParameters() {
        record.setMessage("%s");
        record.setParameters(null);
        assertEquals("%s", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithPercentFormatAndEmptyParameters() {
        record.setMessage("%s");
        record.setParameters(new Object[0]);
        assertEquals("%s", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithBogusPercentFormat() {
        record.setMessage("%0.5s");
        record.setParameters(new Object[] { "hi" });
        assertEquals("%0.5s", instance.formatMessage(record));
    }
}
