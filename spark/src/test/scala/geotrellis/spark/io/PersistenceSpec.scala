package geotrellis.spark.io

import geotrellis.raster._
import com.github.nscala_time.time.Imports._
import geotrellis.spark._
import geotrellis.vector.Extent
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime
import org.scalatest._
import spray.json.JsonFormat
import scala.reflect._

abstract class PersistenceSpec[K: ClassTag, V: ClassTag] extends FunSpec with Matchers { self: TestSparkContext =>
  type Container <: RDD[(K, V)]
  type TestReader = FilteringLayerReader[LayerId, K, Container]
  type TestWriter = Writer[LayerId, Container]
  type TestUpdater = LayerUpdater[LayerId, K, V, Container]
  type TestDeleter = LayerDeleter[LayerId]
  type TestCopier = LayerCopier[LayerId]
  type TestTileReader = Reader[LayerId, Reader[K, V]]

  def sample: Container
  def reader: TestReader
  def writer: TestWriter
  def deleter: TestDeleter
  def copier: TestCopier
  def tiles: TestTileReader

  val layerId = LayerId("sample-" + this.getClass.getName, 1)
  val deleteLayerId = LayerId("deleteSample-" + this.getClass.getName, 1) // second layer to avoid data race
  val copiedLayerId = LayerId("copySample-" + this.getClass.getName, 1)
  lazy val query = reader.query(layerId)
  
  it("should not find layer before write") {
    intercept[LayerNotFoundError] {
      reader.read(layerId)
    }
  }

  it("should not delete layer before write") {
    intercept[LayerNotFoundError] {
      deleter.delete(layerId)
    }
  }

  it("should write a layer") {
    writer.write(layerId, sample)
    writer.write(deleteLayerId, sample)
  }

  it("should read a layer back") {
    val actual = reader.read(layerId).keys.collect()
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
      reader.read(deleteLayerId)
    }
  }

  it ("shouldn't copy a layer which already exists") {
    intercept[LayerExistsError] {
      copier.copy(layerId, layerId)
    }
  }

  it ("shouldn't copy a layer which doesn't exists") {
    intercept[LayerNotFoundError] {
      copier.copy(copiedLayerId, copiedLayerId)
    }
  }

  it("should copy a layer") {
    copier.copy(layerId, copiedLayerId)
    reader.read(copiedLayerId).keys.collect() should contain theSameElementsAs reader.read(layerId).keys.collect()
  }
}
