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

package geotrellis.spark
import geotrellis.spark.utils.SparkUtils

import org.apache.spark.SparkContext
import org.scalatest._
import org.scalatest.BeforeAndAfterAll

trait OnlyIfCanRunSpark extends FunSpec { 
  def ifCanRunSpark(f: =>Unit): Unit = {
    val gtHome = 
      SparkUtils.geoTrellisHome match {
        case Some(path) => path
        case None =>
          val path = new java.io.File("target/scala-2.10/").getAbsolutePath
          SparkUtils.setGeoTrellisHome(path)
          path
      }

    scala.util.Properties.envOrNone("SPARK_HOME") match {
        case Some(sparkHome) =>
          SparkUtils.findGeoTrellisJar(gtHome) match {
            case Some(_) => f
            case None => ignore(s"WARNING: No Geotrellis JAR found at $gtHome" ) { }
          }
        case _ => 
          ignore("WARNING: DEFINE SPARK_HOME and GEOTRELLIS_HOME env variables" ) { }
    }
  }
}

/* 
 * Creates a local SparkContext for use in all tests in a suite 
 */
trait SharedSparkContext extends BeforeAndAfterAll { self: Suite =>

  @transient private var _sc: SparkContext = _

  implicit def sc: SparkContext = {
    if (_sc == null) {
      System.setProperty("spark.master.port", "0")    
      
      _sc = SparkUtils.createSparkContext("local", self.suiteName)      
    }

    _sc
  }

  override def afterAll() {
    _sc.stop
    _sc = null
    System.clearProperty("spark.driver.port")
    super.afterAll()
  }
}
