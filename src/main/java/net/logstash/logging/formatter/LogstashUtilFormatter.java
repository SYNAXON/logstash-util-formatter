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
    protected JsonObjectBuilder encodeFields(LogRecord record) {
        JsonObjectBuilder builder = BUILDER.createObjectBuilder();
        builder.add("timestamp", record.getMillis());
        builder.add("level", record.getLevel().toString());
        builder.add("line_number", getLineNumber(record));
        if (record.getSourceClassName() != null) {
            builder.add("class", record.getSourceClassName());
        } else {
            builder.add("class", "null");
        }
        if (record.getSourceMethodName() != null) {
            builder.add("method", record.getSourceMethodName());
        } else {
            builder.add("method", "null");
        }
        if (record.getThrown() != null) {
            encodeStacktrace(record, builder);
        }
        return builder;
    }

    /**
     * Format the stackstrace.
     *
     * @param record the logrecord which contains the stacktrace
     * @param builder the json object builder to append
     */
    protected void encodeStacktrace(LogRecord record, JsonObjectBuilder builder) {
        if (record.getThrown() != null) {
            if (record.getSourceClassName() != null) {
                builder.add("exception_class", record.getThrown().getClass().getName());
            }
            if (record.getThrown().getMessage() != null) {
                builder.add("exception_message", record.getThrown().getMessage());
            }
            if (record.getThrown().getStackTrace().length > 0) {
                StringBuilder strace = new StringBuilder();
                StackTraceElement[] traces = record.getThrown().getStackTrace();
                for (StackTraceElement trace : traces) {
                    strace.append("\t").append(trace.toString()).append("\n");
                }
                builder.add("stacktrace", strace.toString());
            }
        }
    }

    /**
     * Get the line number of the exception.
     *
     * @param record the logrecord
     * @return the line number
     */
    protected int getLineNumber(LogRecord record) {
        int lineNumber = 0;
        if (record.getThrown() != null) {
            StackTraceElement[] traces = record.getThrown().getStackTrace();
            if (traces.length > 0 && traces[0] != null) {
                lineNumber = traces[0].getLineNumber();
            }
        }
        return lineNumber;
    }
}
