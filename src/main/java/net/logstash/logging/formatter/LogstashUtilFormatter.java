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
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.google.gson.*;

/**
 * Formats java.util.logging events for logstash
 * @author James Stauffer
 */
public class LogstashUtilFormatter extends Formatter {

    public LogstashUtilFormatter() {
    }

    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
    private static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setDateFormat(DATE_FORMAT).setPrettyPrinting().create();
    private static String hostName;

    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown-host";
        }
    }

    @Override
    public final String format(final LogRecord record) {
        JsonObject jsonObject = new JsonObject();
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final String dateString = dateFormat.format(new Date(record.getMillis()));
        jsonObject.addProperty("timestamp", dateString);
        jsonObject.addProperty("message", record.getMessage());
        jsonObject.addProperty("source", record.getLoggerName());
        jsonObject.addProperty("source_host", hostName);
        jsonObject.add("level", new JsonPrimitive(record.getLevel().toString()));
        jsonObject.add("class", new JsonPrimitive(record.getSourceClassName()));
        jsonObject.add("method", new JsonPrimitive(record.getSourceMethodName()));
        if (record.getThrown() != null) {
            jsonObject.add("line_number", new JsonPrimitive(getLineNumberFromStackTrace(record.getThrown().getStackTrace())));
            JsonObject jsonExceptionObject = new JsonObject();

            if (record.getSourceClassName() != null) {
                jsonExceptionObject.add("exception_class", new JsonPrimitive(record.getThrown().getClass().getName()));
            }
            if (record.getThrown().getMessage() != null) {
                jsonExceptionObject.add("exception_message", new JsonPrimitive(record.getThrown().getMessage()));
            }
            final StackTraceElement[] traces = record.getThrown().getStackTrace();
            if (traces.length > 0) {
                StringBuilder strace = new StringBuilder();
                for (StackTraceElement trace : traces) {
                    strace.append("\t").append(trace.toString()).append("\n");
                }
                jsonExceptionObject.add("stacktrace", new JsonPrimitive(strace.toString()));
            }
            jsonObject.add("exception", jsonExceptionObject);
        }
        
        return gson.toJson(jsonObject) + "\n";
    }

    /**
     * Gets line number from stack trace.
     * @param traces all stack trace elements
     * @return line number of the first stacktrace.
     */
    final private int getLineNumberFromStackTrace(final StackTraceElement[] traces) {
        final int lineNumber;
        if (traces.length > 0 && traces[0] != null) {
            lineNumber = traces[0].getLineNumber();
        } else {
            lineNumber = 0;
        }
        return lineNumber;
    }
}
