#!/usr/bin/env bash

startStop=$1
shift
class=$1
shift

home=`dirname "$0"`
home=`cd "$home/.."; pwd`

CLASS_PATH=""
JAVA_OPTS="-Dspark.akka.logLifecycleEvents=true -Xms512m -Xmx2g"

ASSEMBLY_JAR=`ls "$home"/assembly/spark-assembly*hadoop*.jar`

pid=/tmp/spark-$class.pid
log="$home/logs/spark-$class.out"

case "$class" in
  'master')
    class="org.apache.spark.deploy.master.Master"
    ;;
  'worker')
    class="org.apache.spark.deploy.worker.Worker"
    ;;
esac


case $startStop in
  (start)
    java_command="java -cp $ASSEMBLY_JAR $JAVA_OPTS $class $@"
    nohup $java_command >> "$log" 2>&1 < /dev/null &
    echo "Assembly: $ASSEMBLY_JAR"
    echo "Java Command: $java_command"
    echo "Log: $log"
    newpid=$!
    echo $newpid > $pid
    sleep 2
    # Check if the process has died; in that case we'll tail the log so the user can see
    if ! kill -0 $newpid >/dev/null 2>&1; then
      echo "failed to launch $class"
    fi
    ;;
  (stop)
    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo "Stopping $class"
        kill `cat $pid`
      else
        echo "PID not found `cat $pid`"
      fi
    else
      echo "PID file not found: $pid"
    fi
    ;;
esac