package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import org.apache.spark.rdd.RDD
import org.scalatest._
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

case class PersistenceSpecLayerIds(layerId: LayerId, deleteLayerId: LayerId, copiedLayerId: LayerId, movedLayerId: LayerId, reindexedLayerId: LayerId)

abstract class PersistenceSpec[
  K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag,
  M: JsonFormat
] extends FunSpec with Matchers { self: FunSpec =>
  private var additionalSpecs: List[PersistenceSpecLayerIds => Unit] = List()
  def addSpecs(f: PersistenceSpecLayerIds => Unit): Unit =
    additionalSpecs = f :: additionalSpecs

  type TestReader = FilteringLayerReader[LayerId]
  type TestWriter = LayerWriter[LayerId]
  type TestDeleter = LayerDeleter[LayerId]
  type TestCopier = LayerCopier[LayerId]
  type TestMover = LayerMover[LayerId]
  type TestReindexer = LayerReindexer[LayerId]
  type TestTileReader = Reader[LayerId, Reader[K, V]]

  def sample: RDD[(K, V)] with Metadata[M]
  def reader: TestReader
  def writer: TestWriter
  def deleter: TestDeleter
  def copier: TestCopier
  def mover: TestMover
  def reindexer: TestReindexer
  def tiles: TestTileReader

  def keyIndexMethods: Map[String, KeyIndexMethod[K]]

  def getLayerIds(keyIndexMethod: String): PersistenceSpecLayerIds = {
    val suffix = keyIndexMethod.replace(" ", "_")
    val layerId = LayerId(s"sample-${getClass.getName}-${suffix}", 1)
    val deleteLayerId = LayerId(s"deleteSample-${getClass.getName}-${suffix}", 1) // second layer to avoid data race
    val copiedLayerId = LayerId(s"copySample-${getClass.getName}-${suffix}", 1)
    val movedLayerId = LayerId(s"moveSample-${getClass.getName}-${suffix}", 1)
    val reindexedLayerId = LayerId(s"reindexedSample-${getClass.getName}-${suffix}", 1)
    PersistenceSpecLayerIds(layerId, deleteLayerId, copiedLayerId, movedLayerId, reindexedLayerId)
  }

  for((keyIndexMethodName, keyIndexMethod: KeyIndexMethod[K]) <- keyIndexMethods) {
    describe(s"using key index method ${keyIndexMethodName}") {
      lazy val layerIds @ PersistenceSpecLayerIds(layerId, deleteLayerId, copiedLayerId, movedLayerId, reindexedLayerId) = getLayerIds(keyIndexMethodName)
      lazy val query = reader.query[K, V, M](layerId)

      it("should not find layer before write") {
        intercept[LayerNotFoundError] {
          reader.read[K, V, M](layerId)
        }
      }

      it("should not delete layer before write") {
        intercept[LayerNotFoundError] {
          deleter.delete(layerId)
        }
      }

      it("should write a layer") {
        val keyBounds: KeyBounds[K] = Bounds.fromRdd(sample).get
        writer.write[K, V, M](layerId, sample, keyIndexMethod, keyBounds)
        writer.write[K, V, M](deleteLayerId, sample, keyIndexMethod, keyBounds)
      }

      it("should read a layer back") {
        val actual = reader.read[K, V, M](layerId).keys.collect()
        val expected = sample.keys.collect()

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }

      it("should read a single value") {
        val tileReader = tiles.read(layerId)
        val key = sample.keys.first()
        val readV: V = tileReader.read(key)
        val expectedV: V = sample.filter(_._1 == key).values.first()
        readV should be equals expectedV
      }

      it("should delete a layer") {
        deleter.delete(deleteLayerId)
        intercept[LayerNotFoundError] {
          reader.read[K, V, M](deleteLayerId)
        }
      }

      it("shouldn't copy a layer which already exists") {
        intercept[LayerExistsError] {
          copier.copy[K, V, M](layerId, layerId)
        }
      }

      it("should copy a layer") {
        copier.copy[K, V, M](layerId, copiedLayerId)
        reader.read[K, V, M](copiedLayerId).keys.collect() should contain theSameElementsAs reader.read[K, V, M](layerId).keys.collect()
      }

      it("shouldn't move a layer which already exists") {
        intercept[LayerExistsError] {
          mover.move[K, V, M](layerId, layerId)
        }
      }

      it("should move a layer") {
        val keysBeforeMove = reader.read[K, V, M](layerId).keys.collect()
        mover.move[K, V, M](layerId, movedLayerId)
        intercept[LayerNotFoundError] {
          reader.read[K, V, M](layerId)
        }
        keysBeforeMove should contain theSameElementsAs reader.read[K, V, M](movedLayerId).keys.collect()
        mover.move[K, V, M](movedLayerId, layerId)
      }

      it("should not reindex a layer which doesn't exist") {
        intercept[LayerNotFoundError] {
          reindexer.reindex[K, V, M](movedLayerId, keyIndexMethods.head._2)
        }
      }

      it("should reindex a layer") {
        for((n, reindexMethod) <- keyIndexMethods.filter(_._1 != keyIndexMethodName)) {
          val rid = reindexedLayerId.copy(name = s"""${reindexedLayerId.name}-reindex-${n.replace(" ", "_")}""")
          withClue(s"Failed on method $n") {
            copier.copy[K, V, M](layerId, rid)
            reindexer.reindex[K, V, M](rid, reindexMethod)
            // RETODO: We need to test better
            reader.read[K, V, M](rid).keys.collect() should contain theSameElementsAs reader.read[K, V, M](layerId).keys.collect()
          }
        }
      }

      for(f <- additionalSpecs) {
        f(layerIds)
      }
    }
  }
}
