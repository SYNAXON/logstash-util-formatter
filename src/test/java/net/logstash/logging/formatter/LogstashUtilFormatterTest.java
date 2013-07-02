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

    public static final int LINE_NUMBER = 42;
    private LogRecord record = null;
    private LogstashUtilFormatter instance = new LogstashUtilFormatter();
    private String fullLogMessage = null;
    private JsonObjectBuilder fieldsBuilder;
    private JsonObjectBuilder builder;
    private JsonObjectBuilder exceptionBuilder;
    private Exception ex;
    private static String hostName;

    static {
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown-host";
        }
    }

    /**
     *
     */
    @Before
    public void setUp() {
        long millis = System.currentTimeMillis();
        record = new LogRecord(Level.ALL, "Junit Test");
        record.setLoggerName(LogstashUtilFormatter.class.getName());
        record.setSourceClassName(LogstashUtilFormatter.class.getName());
        record.setSourceMethodName("testMethod");
        record.setMillis(millis);

        ex = new Exception("That is an exception");
        StackTraceElement[] stackTrace = new StackTraceElement[1];
        stackTrace[0] = new StackTraceElement("Test", "methodTest", "Test.class", LINE_NUMBER);
        ex.setStackTrace(stackTrace);
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
        fieldsBuilder.add("line_number", LINE_NUMBER);
        fieldsBuilder.add("class", LogstashUtilFormatter.class.getName());
        fieldsBuilder.add("method", "testMethod");
        fieldsBuilder.add("exception_class", ex.getClass().getName());
        fieldsBuilder.add("exception_message", ex.getMessage());
        fieldsBuilder.add("stacktrace", "\t" + stackTrace[0].toString() + "\n");

        exceptionBuilder = Json.createBuilderFactory(null).createObjectBuilder();
        exceptionBuilder.add("exception_class", ex.getClass().getName());
        exceptionBuilder.add("exception_message", ex.getMessage());
        exceptionBuilder.add("stacktrace", "\t" + stackTrace[0].toString() + "\n");

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
        System.out.println("format");
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
        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        instance.addThrowableInfo(record, result);
        assertEquals(exceptionBuilder.build().toString(), result.build().toString());
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
    public void testAddThrowableInfoThrowableAttachedButWithoutStackTrace() {
        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        record.getThrown().setStackTrace(new StackTraceElement[0]);
        instance.addThrowableInfo(record, result);
        assertEquals("{\"exception_class\":\"java.lang.Exception\",\"exception_message\":\"That is an exception\"}", result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoThrowableAttachedButWithoutSourceClassName() {
        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        record.getThrown().setStackTrace(new StackTraceElement[0]);
        record.setSourceClassName(null);
        instance.addThrowableInfo(record, result);
        assertEquals("{\"exception_message\":\"That is an exception\"}", result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoThrowableAttachedButWithoutMessage() {
        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        record.setThrown(new Exception());
        record.getThrown().setStackTrace(new StackTraceElement[0]);
        instance.addThrowableInfo(record, result);
        assertEquals("{\"exception_class\":\"java.lang.Exception\"}", result.build().toString());
    }

    /**
     * Test of getLineNumber method, of class LogstashFormatter.
     */
    @Test
    public void testGetLineNumber() {
        int result = instance.getLineNumber(record);
        assertEquals(LINE_NUMBER, result);
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
}
