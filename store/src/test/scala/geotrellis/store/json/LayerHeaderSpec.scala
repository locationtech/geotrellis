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

package geotrellis.store.json

import geotrellis.store._
import geotrellis.store.hadoop.HadoopLayerHeader
import geotrellis.store.file.FileLayerHeader

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

import java.net.URI

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class LayerHeaderSpec extends AnyFunSpec with Matchers {
  val hadoopHeader = HadoopLayerHeader("key", "value", new URI("hdfs:/path/to"))
  val fileHeader = FileLayerHeader("key", "value", "/path/to")


  def roundTrip[T: Encoder: Decoder](thing: T): Unit = {
    val json = thing.asJson
    val out = json.as[T].valueOr(throw _)
    out should be equals (thing)
  }

  def readAsLayerHeader[T: Encoder: Decoder](thing: T, format: String, key: String, value: String) {
    val json = thing.asJson
    val layerHeader = json.as[LayerHeader].valueOr(throw _)
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
}
