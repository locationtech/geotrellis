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

abstract class PersistenceSpec[K: ClassTag, V: ClassTag] extends FunSpec with Matchers { self: OnlyIfCanRunSpark =>
  type Container <: RDD[(K, V)]
  type TestReader = FilteringLayerReader[LayerId, K, Container]
  type TestWriter = Writer[LayerId, Container]
  type TestUpdater = LayerUpdater[LayerId, K, V, Container]
  type TestDeleter = LayerDeleter[LayerId]
  type TestTileReader = Reader[LayerId, Reader[K, V]]

  def sample: Container
  def reader: TestReader
  def writer: TestWriter
  def deleter: TestDeleter
  def tiles: TestTileReader

  val layerId = LayerId("sample-" + this.getClass.getName, 1)
  val deleteLayerId = LayerId("deleteSample-" + this.getClass.getName, 1) // second layer to avoid data race
  lazy val query = reader.query(layerId)
  
  if (canRunSpark) {

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
  }
}
