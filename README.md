# Java Util Logging JSON encoder for Logstash

## Include as a dependency

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

## Example usage in Jenkins on Debian

* Create a directory in `JENKINS_HOME`: `mkdir /var/lib/jenkins/lib`
* Copy the shaded jar to this directory.
* Create a `logging.properties` in `/var/lib/jenkins/lib`:

```
handlers= java.util.logging.ConsoleHandler,java.util.logging.FileHandler
.level= INFO

java.util.logging.FileHandler.level = INFO
java.util.logging.FileHandler.formatter = net.logstash.logging.formatter.LogstashUtilFormatter
java.util.logging.FileHandler.pattern = /var/log/jenkins/logstash.log
java.util.logging.FileHandler.limit = 5000000
java.util.logging.FileHandler.count = 1

java.util.logging.ConsoleHandler.level = INFO
java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
```
* Extend `JAVA_ARGS` in `/etc/default/jenkins`:

```
JAVA_ARGS="$JAVA_ARGS -Djava.endorsed.dirs=$JENKINS_HOME/lib -Djava.util.logging.config.file=$JENKINS_HOME/lib/logging.properties"
```

* Use it in your logstash configuration like this:

```
input {
  file {
    type => "jenkins-server"
    path => "/var/log/jenkins/logstash.log"
    format => "json_event"
  }
}
```
