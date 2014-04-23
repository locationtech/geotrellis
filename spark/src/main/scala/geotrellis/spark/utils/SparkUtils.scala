/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.utils
import org.apache.hadoop.conf.Configuration
import org.apache.spark.Logging
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import java.io.File

object SparkUtils extends Logging {
   
  def createSparkConf = new SparkConf()
  
  def createSparkContext(sparkMaster: String, appName: String, sparkConf: SparkConf = createSparkConf) = {
    val sparkHome = scala.util.Properties.envOrNone("SPARK_HOME") match {
      case Some(value) => value
      case None        => throw new Error("Oops, SPARK_HOME is not defined")
    }

    val gtHome = scala.util.Properties.envOrNone("GEOTRELLIS_HOME") match {
      case Some(value) => value
      case None        => throw new Error("Oops, GEOTRELLIS_HOME is not defined")
    }
   
    sparkConf
    .setMaster(sparkMaster)
    .setAppName(appName)
    .setSparkHome(sparkHome)
    .setJars(Array(jar(gtHome)))
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryo.registrator", "geotrellis.spark.KryoRegistrator")
    
    // TODO - find a way to have command line pass these in
    //.set("io.map.index.interval", "1")
    //.set("dfs.replication","1")
    //.set("spark.akka.timeout","10000")

    new SparkContext(sparkConf)
  }
  
  def createHadoopConfiguration = {
    // TODO - figure out how to get the conf directory automatically added to classpath via sbt
    // - right now it is manually added
    Configuration.addDefaultResource("core-site.xml")
    Configuration.addDefaultResource("mapred-site.xml")
    Configuration.addDefaultResource("hdfs-site.xml")
    new Configuration
  }

  /* 
   * Find the geotrellis-spark jar under the geotrellis home directory and return it with a 
   * "local:" prefix that tells Spark to look for that jar on every slave node where its 
   * executors are scheduled
   * 
   *  For e.g., if GEOTRELLIS_HOME = /usr/local/geotrellis 
   *  and the jar lives in /usr/local/geotrellis/geotrellis-spark_2.10-0.10.0-SNAPSHOT.jar
   *  then this gets returned: "local:/usr/local/geotrellis/geotrellis-spark_2.10-0.10.0-SNAPSHOT.jar" 
   **/
  def jar(gtHome: String): String = {
    def isMatch(fileName: String): Boolean = "geotrellis-spark(.)*.jar".r.findFirstIn(fileName) match {
      case Some(_) => true
      case None    => false
    }

    def findJar(file: File): Array[Option[File]] = {
      if (isMatch(file.getName))
        Array(Some(file))
      else if (file.isDirectory())
        file.listFiles().flatMap(findJar(_))
      else Array(None)
    }

    def prefix(s: String) = "local:" + s
    
    val matches = findJar(new File(gtHome)).flatten
    if (matches.length == 1) {
      val firstMatch = prefix(matches(0).getAbsolutePath)      
      logInfo(s"Found unique match for geotrellis-spark jar: ${firstMatch}")
      firstMatch
    } else if (matches.length > 1) {
      logInfo(s"Found ${matches.length} matches for geotrellis-spark jar: ")
      logInfo("{" + matches.mkString(",") + "}")
      val firstMatch = prefix(matches(0).getAbsolutePath)
      logInfo("Using first match: " + firstMatch)
      firstMatch
    } else {
      sys.error("Couldn't find geotrellis jar")
    }
  }
}
