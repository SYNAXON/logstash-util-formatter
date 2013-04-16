# Java Util Logging JSON encoder for Logstash

First, add it to your project as a dependency.

Maven style:

```xml
<dependency>
  <groupId>net.logstash.logging</groupId>
  <artifactId>logstash-util-formatter</artifactId>
  <version>1.0</version>
</dependency>
```

Use it in your `logging.properties` like this:

```
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.formatter=net.logstash.logging.formatter.LogstashUtilFormatter

```

Use it in your logstash configuration like this:

```
input {
  file {
    type => "your-log-type"
    path => "/some/path/to/your/file.log"
    format => "json_event"
  }
}
```
