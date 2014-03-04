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

        Exception ex = new Exception("That is an exception");
        StackTraceElement[] stackTrace = new StackTraceElement[1];
        stackTrace[0] = new StackTraceElement("Test", "methodTest", "Test.class", LINE_NUMBER);
        ex.setStackTrace(stackTrace);
        record.setThrown(ex);
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testFormat() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(LogstashUtilFormatter.DATE_FORMAT);
        final String dateString = dateFormat.format(new Date(record.getMillis()));
        String expected = "{"
            + "\"timestamp\":\"" + dateString + "\","
            + "\"message\":\"Junit Test\","
            + "\"source\":\"net.logstash.logging.formatter.LogstashUtilFormatter\","
            + "\"source_host\":\"" + hostName + "\","
            + "\"level\":\"ALL\","
            + "\"class\":\"net.logstash.logging.formatter.LogstashUtilFormatter\","
            + "\"method\":\"testMethod\","
            + "\"line_number\":" + LINE_NUMBER + ","
            + "\"exception\":{"
            + "\"exception_class\":\"java.lang.Exception\","
            + "\"exception_message\":\"That is an exception\","
            + "\"stacktrace\":\"\\tTest.methodTest(Test.class:42)\\n\""
            + "}"
            + "}\n";
        assertEquals(expected, instance.format(record));
    }
}
