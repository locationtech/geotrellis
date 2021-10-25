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

package geotrellis.spark.store

import geotrellis.raster.histogram._
import geotrellis.store.{LayerId, AttributeStore}
import geotrellis.spark._
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import _root_.io.circe.generic.JsonCodec

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

abstract class AttributeStoreSpec extends AnyFunSpec with Matchers with TestEnvironment with TestFiles {

  def attributeStore: AttributeStore

  it("should write to an attribute store") {
    attributeStore.write(LayerId("test1", 1), "metadata", "test")
    attributeStore.write(LayerId("test2", 2), "metadata", "test")
    attributeStore.write(LayerId("test2", 2), "metadata", "test")
    attributeStore.write(LayerId("test3", 3), "metadata", "test")
  }

  it("should know that these new IDs exist") {
    attributeStore.layerExists(LayerId("test1", 1)) should be (true)
    attributeStore.layerExists(LayerId("test2", 2)) should be (true)
    attributeStore.layerExists(LayerId("test3", 3)) should be (true)
  }

  it("should read layer ids") {
    attributeStore.layerIds.sortBy(_.zoom).toList should be (List(LayerId("test1", 1), LayerId("test2", 2), LayerId("test3", 3)))
  }

  it("should clear out the attribute store") {
    attributeStore
      .layerIds
      .foreach(attributeStore.delete(_))

    attributeStore
      .layerIds.isEmpty should be (true)
  }

  it("should save and pull out a histogram") {
    val layerId = LayerId("test", 3)
    val histo = DecreasingTestFile.histogramExactInt

    attributeStore.write(layerId, "histogram", histo)

    val loaded = attributeStore.read[Histogram[Int]](layerId, "histogram")
    loaded.mean() should be (histo.mean())
  }

  it("should save and load a random RootJsonReadable object") {
    val layerId = LayerId("test", 3)
    @JsonCodec
    case class Foo(x: Int, y: String)

    val foo = Foo(1, "thing")

    attributeStore.write(layerId, "foo", foo)
    attributeStore.read[Foo](layerId, "foo") should be (foo)
  }
}
