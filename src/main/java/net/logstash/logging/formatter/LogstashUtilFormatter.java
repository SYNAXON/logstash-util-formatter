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
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 */
public class LogstashUtilFormatter extends Formatter {

    private static final JsonBuilderFactory BUILDER = Json.createBuilderFactory(null);

    private static String hostName;

    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown-host";
        }
    }

    @Override
    public String format(LogRecord record) {
        String dateString = ISODateTimeFormat.dateTime().print(record.getMillis());
        return BUILDER
                .createObjectBuilder()
                .add("@timestamp", dateString)
                .add("@message", record.getMessage())
                .add("@source", record.getLoggerName())
                .add("@source_host", hostName)
                .add("@fields", encodeFields(record))
                .build()
                .toString() + "\n";
    }

    /**
     * Enocde all addtional fields.
     *
     * @param record the log record
     * @return objectBuilder
     */
    JsonObjectBuilder encodeFields(LogRecord record) {
        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        builder.add("timestamp", record.getMillis());
        builder.add("level", record.getLevel().toString());
        builder.add("line_number", getLineNumber(record));
        addSourceClassName(record, builder);
        addSourceMethodName(record, builder);
        addThrowableInfo(record, builder);
        return builder;
    }

    /**
     * Format the stackstrace.
     *
     * @param record the logrecord which contains the stacktrace
     * @param builder the json object builder to append
     */
    void addThrowableInfo(LogRecord record, JsonObjectBuilder builder) {
        if (record.getThrown() != null) {
            if (record.getSourceClassName() != null) {
                builder.add("exception_class", record.getThrown().getClass().getName());
            }
            if (record.getThrown().getMessage() != null) {
                builder.add("exception_message", record.getThrown().getMessage());
            }
            addStacktraceElements(record, builder);
        }
    }

    /**
     * Get the line number of the exception.
     *
     * @param record the logrecord
     * @return the line number
     */
    int getLineNumber(LogRecord record) {
        final int lineNumber;
        if (record.getThrown() != null) {
            lineNumber = getLineNumberFromStackTrace(record.getThrown().getStackTrace());
        } else {
            lineNumber = 0;
        }
        return lineNumber;
    }

    int getLineNumberFromStackTrace(final StackTraceElement[] traces) {
        final int lineNumber;
        if (traces.length > 0 && traces[0] != null) {
            lineNumber = traces[0].getLineNumber();
        } else {
            lineNumber = 0;
        }
        return lineNumber;
    }

    private void addSourceMethodName(LogRecord record, JsonObjectBuilder builder) {
        addValue(builder, "method", record.getSourceMethodName());
    }

    private void addSourceClassName(LogRecord record, JsonObjectBuilder builder) {
        addValue(builder, "class", record.getSourceClassName());
    }

    private void addStacktraceElements(LogRecord record, JsonObjectBuilder builder) {
        final StackTraceElement[] traces = record.getThrown().getStackTrace();
        if (traces.length > 0) {
            StringBuilder strace = new StringBuilder();
            for (StackTraceElement trace : traces) {
                strace.append("\t").append(trace.toString()).append("\n");
            }
            builder.add("stacktrace", strace.toString());
        }
    }

    void addValue(JsonObjectBuilder builder, final String key, final String value) {
        if (value != null) {
            builder.add(key, value);
        } else {
            builder.add(key, "null");
        }
    }
}
