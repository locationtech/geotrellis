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
import scala.util._

trait OnlyIfCanRunSpark extends FunSpec with BeforeAndAfterAll {
  @transient private var _sc: Try[SparkContext] = 
    Failure(new Exception("SparContext not yet initilized"))

  implicit def sc: SparkContext = _sc.get

  System.setProperty("spark.master.port", "0")    
  _sc = Try{SparkUtils.createSparkContext("local", this.suiteName)}
  
  def ifCanRunSpark(f: => Unit): Unit = {    
     _sc match {
      case Success(sc) => f
      case Failure(error) => ignore(error.getMessage) {}
    }    
  }
  
  override def afterAll() {
    _sc match {
      case Success(sc) => sc.stop
      case Failure(err) => 
    } 
    System.clearProperty("spark.driver.port")
    super.afterAll()
  }
}
