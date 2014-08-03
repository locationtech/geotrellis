#/bin/bash

ASSEMBLY_JAR=geotrellis-spark-assembly-0.10.0-SNAPSHOT.jar
ASSEMBLY_DIR=~/geotrellis/spark/target/scala-2.10

#Strip the signatures from the jar, else it won't load
zip -d $ASSEMBLY_DIR/$ASSEMBLY_JAR META-INF/*.SF
zip -d $ASSEMBLY_DIR/$ASSEMBLY_JAR META-INF/*.RSA

{% for host in spark_workers %}
ssh ubuntu@{{host}} mkdir -p $ASSEMBLY_DIR
{% endfor %}

#PUSH!
{% for host in spark_workers %}
rsync -v -e ssh $ASSEMBLY_DIR/$ASSEMBLY_JAR ubuntu@{{host}}:$ASSEMBLY_DIR/$ASSEMBLY_JAR
{% endfor %}
