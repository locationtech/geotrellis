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
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest._
import org.scalatest.BeforeAndAfterAll
import scala.util._

object OnlyIfCanRunSpark {
  lazy val sc = {
    System.setProperty("spark.driver.port", "0")
    System.setProperty("spark.hostPort", "0")

    val sparkContext = SparkUtils.createLocalSparkContext("local[1]", "Test Context", new SparkConf())

    System.clearProperty("spark.driver.port")
    System.clearProperty("spark.hostPort")

    sparkContext
  }
}

trait OnlyIfCanRunSpark extends FunSpec with BeforeAndAfterAll {
  implicit def sc: SparkContext = OnlyIfCanRunSpark.sc

  def canRunSpark: Boolean = true

  def ifCanRunSpark(f: => Unit): Unit = {    
    f
  }  
}
