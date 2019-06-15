/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.util

import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import java.io.File

object SparkUtils extends LazyLogging {
  def createSparkConf = new SparkConf()

  private val gtHomeLock = new Object()
  private var _geoTrellisHome: Option[String] = None
  def geoTrellisHome = _geoTrellisHome
  def setGeoTrellisHome(path: String): Unit =
    _geoTrellisHome match {
      case Some(s) =>
        if(s != _geoTrellisHome) {
          sys.error(s"GeoTrellis Home directory already set to $s")
        }
      case None =>
        gtHomeLock.synchronized {
          _geoTrellisHome = Some(path)
        }
    }

  def createLocalSparkContext(sparkMaster: String, appName: String, sparkConf: SparkConf = createSparkConf) = {
    sparkConf
      .setMaster(sparkMaster)
      .setAppName(appName)
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", classOf[geotrellis.spark.store.kryo.KryoRegistrator].getName)

    new SparkContext(sparkConf)
  }

  /**
   * This overload is to be used with spark-submit, which will provide the rest of the conf
   */
  def createSparkContext(appName: String, sparkConf: SparkConf = createSparkConf) = {
    sparkConf
      .setAppName(appName)
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", classOf[geotrellis.spark.store.kryo.KryoRegistrator].getName)

    new SparkContext(sparkConf)
  }


  /**
   * This is basically unused now.
   * The only possible reasons to use this is to include HDFS config with the jar.
   * That seems strange and confusing, we do not do that.
   */
  def hadoopConfiguration = {
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
  def findGeoTrellisJar(gtHome: String): Option[String] = {
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
      val firstMatch = matches(0).getAbsolutePath
      logger.info(s"Found unique match for geotrellis-spark jar: ${firstMatch}")
      Some(firstMatch)
    } else if (matches.length > 1) {
      logger.info(s"Found ${matches.length} matches for geotrellis-spark jar: ")
      logger.info("{" + matches.mkString(",") + "}")
      val firstMatch = prefix(matches(0).getAbsolutePath)
      logger.info("Using first match: " + firstMatch)
      Some(firstMatch)
    } else {
      None
    }
  }
}
