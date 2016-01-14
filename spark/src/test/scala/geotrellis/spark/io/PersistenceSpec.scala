package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import org.apache.spark.rdd.RDD
import org.scalatest._
import scala.reflect._

abstract class PersistenceSpec[K: ClassTag, V: ClassTag, M] extends FunSpec with Matchers { self: FunSpec =>
  type TestReader = FilteringLayerReader[LayerId, K, M, RDD[(K, V)] with Metadata[M]]
  type TestWriter = Writer[LayerId, K, RDD[(K, V)] with Metadata[M]]
  type TestUpdater = LayerUpdater[LayerId, K, V, M]
  type TestDeleter = LayerDeleter[LayerId]
  type TestCopier = LayerCopier[LayerId, K]
  type TestMover = LayerMover[LayerId, K]
  type TestReindexer = LayerReindexer[LayerId, K]
  type TestTileReader = Reader[LayerId, Reader[K, V]]

  def sample: RDD[(K, V)] with Metadata[M]
  def reader: TestReader
  def writer: TestWriter
  def deleter: TestDeleter
  def copier: TestCopier
  def mover: TestMover
  def reindexer: TestReindexer
  def tiles: TestTileReader

  val writerKeyIndexMethod: KeyIndexMethod[K]
  val reindexerKeyIndexMethod: KeyIndexMethod[K]
  val layerId = LayerId("sample-" + this.getClass.getName, 1)
  val deleteLayerId = LayerId("deleteSample-" + this.getClass.getName, 1) // second layer to avoid data race
  val copiedLayerId = LayerId("copySample-" + this.getClass.getName, 1)
  val movedLayerId = LayerId("moveSample-" + this.getClass.getName, 1)
  val reindexedLayerId = LayerId("reindexedSample-" + this.getClass.getName, 1)
  lazy val query = reader.query(layerId)

  it("should not find layer before write") {
    intercept[LayerNotFoundError] {
      reader.read[KeyIndex[K]](layerId)
    }
  }

  it("should not delete layer before write") {
    intercept[LayerNotFoundError] {
      deleter.delete(layerId)
    }
  }

  it("should write a layer") {
    writer.write(layerId, sample, writerKeyIndexMethod)
    writer.write(deleteLayerId, sample, writerKeyIndexMethod)
  }

  it("should read a layer back") {
    val actual = reader.read[KeyIndex[K]](layerId).keys.collect()
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
      reader.read[KeyIndex[K]](deleteLayerId)
    }
  }

  it("shouldn't copy a layer which already exists") {
    intercept[LayerExistsError] {
      copier.copy[KeyIndex[K]](layerId, layerId)
    }
  }

  it("should copy a layer") {
    copier.copy[KeyIndex[K]](layerId, copiedLayerId)
    reader.read[KeyIndex[K]](copiedLayerId).keys.collect() should contain theSameElementsAs reader.read[KeyIndex[K]](layerId).keys.collect()
  }

  it("shouldn't move a layer which already exists") {
    intercept[LayerExistsError] {
      mover.move[KeyIndex[K]](layerId, layerId)
    }
  }

  it("should move a layer") {
    val keysBeforeMove = reader.read[KeyIndex[K]](layerId).keys.collect()
    mover.move[KeyIndex[K]](layerId, movedLayerId)
    intercept[LayerNotFoundError] {
      reader.read[KeyIndex[K]](layerId)
    }
    keysBeforeMove should contain theSameElementsAs reader.read[KeyIndex[K]](movedLayerId).keys.collect()
    mover.move[KeyIndex[K]](movedLayerId, layerId)
  }

  it("should not reindex a layer which doesn't exists") {
    intercept[LayerNotFoundError] {
      reindexer.reindex(movedLayerId, reindexerKeyIndexMethod)
    }
  }

  it("should reindex a layer") {
    copier.copy[KeyIndex[K]](layerId, reindexedLayerId)
    reindexer.reindex(reindexedLayerId, reindexerKeyIndexMethod)
  }
}
