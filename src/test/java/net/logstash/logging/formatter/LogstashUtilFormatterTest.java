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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        String dateString = formatter.format(new Date(millis));
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

        exceptionBuilder = Json.createBuilderFactory(null).createObjectBuilder();
        exceptionBuilder.add("exception_class", ex.getClass().getName());
        exceptionBuilder.add("exception_message", ex.getMessage());
        exceptionBuilder.add("stacktrace", "\t" + stackTrace[0].toString() + "\n");

        fieldsBuilder.add("exception", exceptionBuilder);

        builder.add("@fields", fieldsBuilder);

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
     * Test of encodeStacktrace method, of class LogstashFormatter.
     */
    @Test
    public void testEncodeStacktrace() {
        JsonObjectBuilder result = instance.encodeStacktrace(record);
        assertEquals(exceptionBuilder.build().toString(), result.build().toString());
    }

    /**
     * Test of getLineNumber method, of class LogstashFormatter.
     */
    @Test
    public void testGetLineNumber() {
        int result = instance.getLineNumber(record);
        assertEquals(LINE_NUMBER, result);
    }
}
