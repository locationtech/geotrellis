package geotrellis.spark.io.cassandra

import java.io.IOException

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.raster.op.local._
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.testfiles._
import geotrellis.proj4.LatLng

import org.apache.spark._
import org.apache.spark.rdd._
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.Matchers._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path

import com.github.nscala_time.time.Imports._

class CassandraRasterCatalogSpec extends FunSpec
    with RasterRDDMatchers
    with TestFiles
    with TestEnvironment
    with OnlyIfCanRunSpark
    with SharedEmbeddedCassandra
    with Logging {

  describe("Cassandra Raster Catalog with Spatial Rasters") {
    ifCanRunSpark { 

      useCassandraConfig("cassandra-default.yaml.template")
      val connector = EmbeddedCassandraConnector(Set(cassandraHost))
      implicit val cassandra = CassandraInstance(connector, "test")

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sc.hadoopGeoTiffRDD(allOnes)

      val layoutScheme = ZoomedLayoutScheme(512)
      val tableName = "tiles"

      val catalog = 
        CassandraRasterCatalog("metadata", "attributes")

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (onesRdd, level) => 
        val layerId = LayerId("ones", level.zoom)

        it("should succeed writing to a table") {
          catalog.writer[SpatialKey](RowMajorKeyIndexMethod, tableName).write(layerId, onesRdd)
        }

        it("should load out saved tiles") {
          val rdd = catalog.reader[SpatialKey].read(layerId)
          rdd.count should be > 0l
        }

        it("should load out a single tile") {
          val key = catalog.reader[SpatialKey].read(layerId).map(_._1).collect.head
          val tile = catalog.tileReader[SpatialKey](layerId).read(key)
          (tile.cols, tile.rows) should be ((512, 512))
        }

        it("should load out saved tiles, but only for the right zoom") {
          intercept[LayerNotFoundError] {
            catalog.reader[SpatialKey].read(LayerId("ones", level.zoom + 1)).count()
          }
        }
        it("fetch a TileExtent from catalog") {
          val tileBounds = GridBounds(915,612,916,612)
          val filters = new FilterSet[SpatialKey] withFilter SpaceFilter(tileBounds)
          val rdd1 = catalog.reader[SpatialKey].read(LayerId("ones", level.zoom), filters)
          val rdd2 = catalog.reader[SpatialKey].read(LayerId("ones", 10), filters)

          val f = rdd1.first.tile
          val f2 = rdd2.first.tile
          logError(f.toString)
          logError(f2.toString)

          val out = rdd1.combinePairs(rdd2) { case (tms1, tms2) =>
            require(tms1.id == tms2.id)
            val res = tms1.tile.localAdd(tms2.tile)
            (tms1.id, res)
          }

          val tile = out.first.tile
          tile.get(497,511) should be (2)
        }      
      }
    }
  }

  describe("Cassandra Raster Catalog with SpaceTime Rasters") {
    ifCanRunSpark { 

      useCassandraConfig("cassandra-default.yaml.template")
      val connector = EmbeddedCassandraConnector(Set(cassandraHost))
      implicit val cassandra = CassandraInstance(connector, "test")

      val tableName = "spacetime_tiles"

      val catalog = 
        CassandraRasterCatalog("metadata", "attributes")

      val zoom = 10
      val layerId = LayerId("coordinates", zoom)

      it("should succeed writing to a table") {
        catalog.writer[SpaceTimeKey](ZCurveKeyIndexMethod.byYear, tableName).write(layerId, CoordinateSpaceTime)
      }

      it("should load out saved tiles") {
        val rdd = catalog.reader[SpaceTimeKey].read(layerId)
        rdd.count should be > 0l
      }

      it("should load out a single tile") {
        val key = catalog.reader[SpaceTimeKey].read(layerId).map(_._1).collect.head
        val tile = catalog.tileReader[SpaceTimeKey](layerId).read(key)
        val actual = CoordinateSpaceTime.collect.toMap.apply(key)
        tilesEqual(tile, actual)
      }

      it("should load out saved tiles, but only for the right zoom") {
        intercept[LayerNotFoundError] {
          catalog.reader[SpaceTimeKey].read(LayerId("coordinates", zoom + 1)).count()
        }
      }

      it("should load a layer with filters on space only") {
        val keys = CoordinateSpaceTime.map(_._1).collect

        val cols = keys.map(_.spatialKey.col)
        val (minCol, maxCol) = (cols.min, cols.max)
        val rows = keys.map(_.spatialKey.row)
        val (minRow, maxRow) = (rows.min, rows.max)

        val tileBounds = GridBounds(minCol + 1, minRow + 1, maxCol, maxRow)

        val filters = 
          new FilterSet[SpaceTimeKey] 
            .withFilter(SpaceFilter(tileBounds))

        val rdd = catalog.reader[SpaceTimeKey].read(LayerId("coordinates", zoom), filters)

        rdd.map(_._1).collect.foreach { case SpaceTimeKey(col, row, time) =>
          tileBounds.contains(col, row) should be (true)
        }
      }

      it("should load a layer with filters on space and time") {
        val keys = CoordinateSpaceTime.map(_._1).collect

        val cols = keys.map(_.spatialKey.col)
        val (minCol, maxCol) = (cols.min, cols.max)
        val rows = keys.map(_.spatialKey.row)
        val (minRow, maxRow) = (rows.min, rows.max)
        val times = keys.map(_.temporalComponent.time)
        val maxTime = times.max

        val tileBounds = GridBounds(minCol + 1, minRow + 1, maxCol, maxRow)

        val filters = 
          new FilterSet[SpaceTimeKey] 
            .withFilter(SpaceFilter(tileBounds))
            .withFilter(TimeFilter(maxTime))

        val rdd = catalog.reader[SpaceTimeKey].read(LayerId("coordinates", zoom), filters)

        rdd.map(_._1).collect.foreach { case SpaceTimeKey(col, row, time) =>
          tileBounds.contains(col, row) should be (true)
          time should be (maxTime)
        }
      }
    }
  }
}
