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

import geotrellis.spark.io.cassandra.SharedEmbeddedCassandra
import geotrellis.spark.utils.SparkUtils
import org.apache.spark.{Logging, SparkConf, SparkContext}
import org.scalatest._
import org.scalatest.BeforeAndAfterAll
import scala.util._

object OnlyIfCanRunSpark extends SharedEmbeddedCassandra with Logging {
  lazy val _sc = Try{
    System.setProperty("spark.driver.port", "0")
    System.setProperty("spark.hostPort", "0")

    val sparkConf = new SparkConf()

    useCassandraConfig(Seq("another-cassandra.yaml"))
    val host = getHost().getHostAddress
    val rpcPort : Int = getRpcPort()
    val nativePort : Int = getNativePort()

    sparkConf.set("spark.cassandra.connection.host", host)
      .set("spark.cassandra.connection.rpc.port", rpcPort.toString)
      .set("spark.cassandra.connection.native.port", nativePort.toString)

    val sparkContext = SparkUtils.createLocalSparkContext("local[8]", "Test Context", sparkConf)

    System.clearProperty("spark.driver.port")
    System.clearProperty("spark.hostPort")

    sparkContext
  }
}

trait OnlyIfCanRunSpark extends FunSpec with BeforeAndAfterAll  {
  import OnlyIfCanRunSpark._

  implicit def sc: SparkContext = _sc.get
  
  def ifCanRunSpark(f: => Unit): Unit = {    
     _sc match {
      case Success(sc) => f
      case Failure(error) => ignore(error.getMessage) {}
    }    
  }  
}
