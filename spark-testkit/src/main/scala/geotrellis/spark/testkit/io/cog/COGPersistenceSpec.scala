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

package geotrellis.spark.testkit.io.cog

import geotrellis.tiling._
import geotrellis.raster.{CellGrid, MultibandTile}
import geotrellis.raster.crop.TileCropMethods
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.resample._
import geotrellis.raster.io.geotiff.compression.NoCompression
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.layers._
import geotrellis.layers.cog.{COGCollectionLayerReader, COGValueReader}
import geotrellis.layers.index._
import geotrellis.layers.json._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.store.cog._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.cog.COGTestFiles
import geotrellis.util._

import org.apache.spark.rdd.RDD
import org.scalatest._

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

abstract class COGPersistenceSpec[
  K: SpatialComponent: Ordering: Boundable: JsonFormat: ClassTag,
  V <: CellGrid[Int]: GeoTiffReader: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: ClassTag: GeoTiffBuilder
] extends FunSpec with Matchers with BeforeAndAfterAll {

  type TestReader = COGLayerReader[LayerId]
  type TestWriter = COGLayerWriter
  // TODO: implement and test all layer functions
  // type TestDeleter = LayerDeleter[LayerId]
  // type TestCopier = LayerCopier[LayerId]
  // type TestMover = LayerMover[LayerId]
  // type TestReindexer = LayerReindexer[LayerId]
  type TestTileReader = COGValueReader[LayerId]
  type TestCollectionReader = COGCollectionLayerReader[LayerId]

  def sample: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]
  def reader: TestReader
  def writer: TestWriter
  // def deleter: TestDeleter
  // def copier: TestCopier
  // def mover: TestMover
  // def reindexer: TestReindexer
  // def updater: TestUpdater
  def tiles: TestTileReader
  def creader: TestCollectionReader

  def keyIndexMethods: Map[String, KeyIndexMethod[K]]

  def getLayerIds(keyIndexMethod: String): PersistenceSpecLayerIds = {
    val suffix = keyIndexMethod.replace(" ", "_")
    val layerId = LayerId(s"COGsample-${getClass.getName}-${suffix}", COGTestFiles.ZOOM_LEVEL)
    val deleteLayerId = LayerId(s"deleteCOGSample-${getClass.getName}-${suffix}", COGTestFiles.ZOOM_LEVEL) // second layer to avoid data race
    val copiedLayerId = LayerId(s"copyCOGSample-${getClass.getName}-${suffix}", COGTestFiles.ZOOM_LEVEL)
    val movedLayerId = LayerId(s"movCOGeSample-${getClass.getName}-${suffix}", COGTestFiles.ZOOM_LEVEL)
    val reindexedLayerId = LayerId(s"reindexedCOGSample-${getClass.getName}-${suffix}", COGTestFiles.ZOOM_LEVEL)
    PersistenceSpecLayerIds(layerId, deleteLayerId, copiedLayerId, movedLayerId, reindexedLayerId)
  }

  def specLayerIds =
    for((keyIndexMethodName, keyIndexMethod: KeyIndexMethod[K]) <- keyIndexMethods) yield {
      PersistenceSpecDefinition(keyIndexMethodName, keyIndexMethod, getLayerIds(keyIndexMethodName))
    }

  for(PersistenceSpecDefinition(keyIndexMethodName, keyIndexMethod, PersistenceSpecLayerIds(layerId, deleteLayerId, copiedLayerId, movedLayerId, reindexedLayerId)) <- specLayerIds) {
    describe(s"using key index method ${keyIndexMethodName}") {
      lazy val query = reader.query[K, V](layerId)

      it("should not find layer before write") {
        intercept[LayerNotFoundError] {
          reader.read[K, V](layerId)
        }
      }

      it("should not find layer before write (collections api)") {
        intercept[LayerNotFoundError] {
          creader.read[K, V](layerId)
        }
      }

      /*it("should throw a layer deletion error if metadata required for deleting a layer's tiles is not found") {
        intercept[LayerDeleteError] {
          deleter.delete(layerId)
        }
      }*/

      it("should write a layer") {
        writer.write[K, V](layerId.name, sample, layerId.zoom, keyIndexMethod)
      }

      it("should read a layer back") {
        val actual = reader.read[K, V](layerId).keys.collect()
        val expected = sample.keys.collect()

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }

      it("should read a subset of bands from the layer") {
        val result =
          reader
            .readSubsetBands[K](layerId, Seq(12, 0, -1))
            .mapValues { v => MultibandTile(v.flatten) }
            .values
            .collect()

        for (band <- result) {
          band.bandCount should be (1)
        }
      }

      it("should read a layer back (collections api)") {
        val actual = creader.read[K, V](layerId).map(_._1)
        val expected = sample.keys.collect()

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }

      it("should read a single value") {
        val tileReader = tiles.reader[K, V](layerId)
        val key = sample.keys.first()
        val readV: V = tileReader.read(key)
        val expectedV: V = sample.filter(_._1 == key).values.first()
        readV should be equals expectedV
      }

      it("should write a layer with maxTileSize set") {
        val tileSize = 1024

        writer.write[K, V](s"${layerId.name}-2", sample, layerId.zoom, keyIndexMethod, tileSize)
      }

      it("should create a COGLayer with the resample method set") {
        COGLayer.fromLayerRDD(sample, layerId.zoom, options = Sum)
      }

      it("should create a COGLayer with the compression set") {
        COGLayer.fromLayerRDD(sample, layerId.zoom, options = NoCompression)
      }

      /*it("should delete a layer") {
        deleter.delete(deleteLayerId)
        intercept[LayerNotFoundError] {
          reader.read[K, V](deleteLayerId)
        }
      }

      it("shouldn't copy a layer which already exists") {
        intercept[LayerExistsError] {
          copier.copy[K, V](layerId, layerId)
        }
      }

      it("should copy a layer") {
        copier.copy[K, V](layerId, copiedLayerId)
        reader.read[K, V](copiedLayerId).keys.collect() should contain theSameElementsAs reader.read[K, V](layerId).keys.collect()
      }

      it("shouldn't move a layer which already exists") {
        intercept[LayerExistsError] {
          mover.move[K, V](layerId, layerId)
        }
      }

      it("should move a layer") {
        val keysBeforeMove = reader.read[K, V](layerId).keys.collect()
        mover.move[K, V](layerId, movedLayerId)
        intercept[LayerNotFoundError] {
          reader.read[K, V](layerId)
        }
        keysBeforeMove should contain theSameElementsAs reader.read[K, V](movedLayerId).keys.collect()
        mover.move[K, V](movedLayerId, layerId)
      }

      it("should not reindex a layer which doesn't exist") {
        intercept[LayerNotFoundError] {
          reindexer.reindex[K, V](movedLayerId, keyIndexMethods.head._2)
        }
      }

      it("should reindex a layer") {
        for ((n, reindexMethod) <- keyIndexMethods.filter(_._1 != keyIndexMethodName)) {
          val rid = reindexedLayerId.copy(name = s"""${reindexedLayerId.name}-reindex-${n.replace(" ", "_")}""")
          withClue(s"Failed on method $n") {
            copier.copy[K, V](layerId, rid)
            reindexer.reindex[K, V](rid, reindexMethod)

            reader.read[K, V](rid).keys.collect() should contain theSameElementsAs reader.read[K, V](layerId).keys.collect()
          }
        }
      }*/
    }
  }
}
