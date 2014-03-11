/**************************************************************************
 * Copyright (c) 2014 Digital Globe.
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
 **************************************************************************/

package geotrellis.spark
import org.apache.spark.SparkContext
import org.scalatest._
import org.scalatest.BeforeAndAfterAll

/* 
 * Creates a local SparkContext for use in all tests in a suite 
 */
trait SharedSparkContext extends BeforeAndAfterAll { self: Suite =>

  @transient private var _sc: SparkContext = _

  def sc: SparkContext = _sc

  override def beforeAll() {
    System.setProperty("spark.master.port", "0")    
    // we don't call the version in SparkUtils as that moves the jar file dependency around
    // and that is not needed for local spark context
    _sc = new SparkContext("local", self.suiteName)
    super.beforeAll()
  }

  override def afterAll() {
    _sc.stop
    _sc = null
    System.clearProperty("spark.driver.port")
    super.afterAll()
  }
}
