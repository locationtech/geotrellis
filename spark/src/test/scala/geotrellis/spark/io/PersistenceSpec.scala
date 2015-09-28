package geotrellis.spark.io

import geotrellis.raster._
import com.github.nscala_time.time.Imports._
import geotrellis.spark._
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime
import org.scalatest._
import scala.reflect._

abstract class PersistenceSpec[K: ClassTag, V: ClassTag] extends FunSpec with Matchers {
  type Container <: RDD[(K, V)]
  type MetaData = reader.MetaDataType
  type TestReader = FilteringLayerReader[LayerId, K, Container]
  type TestWriter = Writer[LayerId, Container]
  type TestTileReader = Reader[LayerId, Reader[K, V]]

  val sample: Container
  val reader: TestReader
  val writer: TestWriter
  val tiles: TestTileReader

  val layerId = LayerId("sample", 1)
  lazy val query = reader.query(layerId)

  it("should not find layer before write"){
    intercept[LayerReadError] {
      reader.read(layerId)
    }
  }

  it("should write a layer"){
    writer.write(layerId, sample)
  }

  it("should read a layer back"){
    val read = reader.read(layerId).collect().toMap
    read should be equals sample.collect().toMap
  }

  it("should read a single V that exists") {
    val tileReader = tiles.read(layerId)
    val key = sample.keys.first()
    info(s"Key: $key")
    val readV: V = tileReader.read(key)
    val expectedV: V = sample.filter(_._1 == key).values.first()
    readV should be equals expectedV
  }
}


trait AllOnesTestTileTests { self: PersistenceSpec[SpatialKey, Tile] =>

  val bounds1 = GridBounds(1,1,3,3)
  val bounds2 = GridBounds(4,5,6,6)


  it("filters past layout bounds") {
    query.where(Intersects(GridBounds(6,2,7,3))).toRDD.keys.collect() should
      contain theSameElementsAs Array(SpatialKey(6, 3), SpatialKey(6,2))
  }

  it("query inside layer bounds") {
    query.where(Intersects(bounds1)).toRDD.keys.collect() should
      contain theSameElementsAs (for ( (x, y) <- bounds1.coords) yield SpatialKey(x,y))
  }

  it("disjoint query on space") {
    query.where(Intersects(bounds1) or Intersects(bounds2)).toRDD.keys.collect() should
      contain theSameElementsAs (for ( (x, y) <- bounds1.coords ++ bounds2.coords) yield SpatialKey(x,y))
  }
}



trait CoordinateSpaceTimeTests { self: PersistenceSpec[SpaceTimeKey, Tile] =>
  val dates = Vector( // all the dates in the layer
    new DateTime(2010,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2011,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2012,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2013,1,1,0,0,0, DateTimeZone.UTC),
    new DateTime(2014,1,1,0,0,0, DateTimeZone.UTC))
  val bounds1 = GridBounds(1,1,3,3)
  val bounds2 = GridBounds(4,5,6,6)


  it("query disjunction on space") {
    query.where(Intersects(bounds1) or Intersects(bounds2)).toRDD.keys.collect() should
      contain theSameElementsAs {
       for {
          (col, row) <- bounds1.coords ++ bounds2.coords
          time <- dates
        } yield SpaceTimeKey(col, row, time)
      }
  }

  it("query disjunction on space and time") {
    query.where(Intersects(bounds1) or Intersects(bounds2))
      .where(Between(dates(0), dates(1)) or Between(dates(3),dates(4))).toRDD.keys.collect() should
      contain theSameElementsAs {
        for {
          (col, row) <- bounds1.coords ++ bounds2.coords
          time <- dates diff Seq(dates(2))
        } yield SpaceTimeKey(col, row, time)
      }
  }
}
