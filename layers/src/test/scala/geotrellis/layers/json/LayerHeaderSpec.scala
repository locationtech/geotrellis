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

package geotrellis.layers.json

//import geotrellis.spark._
//import geotrellis.spark.io._
//import geotrellis.spark.io.file._
//import geotrellis.spark.io.hadoop._

import spray.json._
import spray.json.DefaultJsonProtocol._
import java.net.URI
import org.scalatest._

class LayerHeaderSpec extends FunSpec with Matchers {
  /*
  val hadoopHeader = HadoopLayerHeader("key", "value", new URI("hdfs:/path/to"))
  val fileHeader = FileLayerHeader("key", "value", "/path/to")


  def roundTrip[T: JsonFormat](thing: T): Unit = {
    val json = thing.toJson
    val out = json.convertTo[T]
    out should be equals (thing)
  }

  def readAsLayerHeader[T: JsonFormat](thing: T, format: String, key: String, value: String) {
    val json = thing.toJson
    val layerHeader = json.convertTo[LayerHeader]
    layerHeader.format should be (format)
    layerHeader.keyClass should be (key)
    layerHeader.valueClass should be (value)
  }

  it("reads HadoopLayerHeader as LayerHeader") {
    readAsLayerHeader(hadoopHeader, "hdfs", "key", "value")
  }

  it("reads FileLayerHeade as LayerHeader") {
    readAsLayerHeader(fileHeader, "file", "key", "value")
  }

  it("round trips FileLayerHeader") {
    roundTrip(fileHeader)
  }

  it("round trips HadoopLayerHeader") {
    roundTrip(hadoopHeader)
  }
  */
}
