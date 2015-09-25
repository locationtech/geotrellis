package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.hadoop.formats.{AvroWritable, AvroKeyWritable}
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.tiling._
import geotrellis.proj4.LatLng
import geotrellis.spark.testfiles._

import org.scalatest._
import org.apache.hadoop.fs.Path
import com.github.nscala_time.time.Imports._
import spray.json.JsonFormat

import scala.reflect._

class HadoopRasterCatalogSpec extends FunSpec
    with Matchers
    with RasterRDDMatchers
    with TestEnvironment
    with TestFiles
    with OnlyIfCanRunSpark
{

  describe("HadoopRasterCatalog with SpatialKey Rasters") {
    // helper to verify filtering is working correctly

    ifCanRunSpark {
      val catalogPath = new Path(inputHome, "catalog-spec")
      val fs = catalogPath.getFileSystem(sc.hadoopConfiguration)
      HdfsUtils.deletePath(catalogPath, sc.hadoopConfiguration)

      val attributeStore = HadoopAttributeStore(new Path(catalogPath, "attributes"))

      def resolveQuery[K: JsonFormat: ClassTag](layerId: LayerId, query: RDDQuery[K, RasterMetaData]) =
        query(
          attributeStore.read[RasterMetaData](layerId, Fields.rddMetadata),
          attributeStore.read[KeyBounds[K]](layerId, Fields.keyBounds))

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sc.hadoopGeoTiffRDD(allOnes)
      val layoutScheme = ZoomedLayoutScheme(LatLng, 512)
      val layerId = LayerId("ones", 10)
      val spatialWriter = HadoopLayerWriter[SpatialKey, Tile, RasterRDD](catalogPath, RowMajorKeyIndexMethod)
      val spatialReader = HadoopLayerReader[SpatialKey, Tile, RasterRDD](catalogPath)

      val spaceTimeWriter = HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](catalogPath, ZCurveKeyIndexMethod.byYear)
      val spaceTimeReader = HadoopLayerReader[SpaceTimeKey, Tile, RasterRDD](catalogPath)


      var ran = false

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme) { (onesRdd, zoom) =>
        ran = true

        it("should succeed saving with default Props"){
          spatialWriter.write(LayerId("ones", zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "ones")))
        }


        it("should know when layer exists"){
          attributeStore.layerExists(LayerId("ones", zoom)) should be (true)
          attributeStore.layerExists(LayerId("nope", 100)) should be (false)
        }

        it("should load out saved tiles") {
          info(s"zoom: $zoom")
          val rdd = spatialReader.read(LayerId("ones", zoom))
          rdd.count should be > 0l
          rdd.map(_._1).collect().toSet shouldEqual onesRdd.map(_._1).collect().toSet
        }

        it("should succeed loading with single path Props"){
          spatialReader.query(LayerId("ones", zoom)).toRDD.count should be > 0l
        }

        it("should succeed loading with double path Props"){
          spatialReader.query(LayerId("ones", zoom)).toRDD.count should be > 0l
        }

        it("should load out saved tiles, but only for the right zoom"){
          intercept[AttributeNotFoundError] {
            spatialReader.query(LayerId("ones", 9)).toRDD.count()
          }
        }

        it("should filter out all but 4 tiles") {
          val layerId = LayerId("ones", zoom)
          val tileBounds = GridBounds(915,612,917,613)

          val query = new RDDQuery[SpatialKey, RasterMetaData].where(Intersects(tileBounds))
          val queryKeyBounds = resolveQuery(layerId, query)

          val expected = spatialReader
            .query(layerId)
            .toRDD
            .collect.filter { case (key, _) =>
              queryKeyBounds.includeKey(key)
            }
          val filteredRdd = spatialReader
            .query(LayerId("ones", zoom))
            .where(Intersects(tileBounds))
            .toRDD

          filteredRdd.count should be (expected.size)
        }


        it("should filter out the correct keys") {
          val layerId = LayerId("ones", zoom)
          val tileBounds = GridBounds(915,611,915,613)
          val unfiltered = spatialReader.query(layerId).toRDD
          val filtered = spatialReader.query(layerId).where(Intersects(tileBounds)).toRDD

          val query = new RDDQuery[SpatialKey, RasterMetaData].where(Intersects(tileBounds))
          val queryKeyBounds = resolveQuery(layerId, query)

          val expected = unfiltered.collect.filter { case (key, value) =>
            queryKeyBounds.includeKey(key)
          }.toMap

          val actual = filtered.collect.toMap

          actual.keys should be (expected.keys)

          for(key <- actual.keys) {
            tilesEqual(actual(key), expected(key))
          }
        }

        it("should filter out the correct keys with different grid bounds") {
          val layerId = LayerId("ones", zoom)
          val tileBounds = GridBounds(915,612,917,613)
          val query = new RDDQuery[SpatialKey, RasterMetaData].where(Intersects(tileBounds))
          val unfiltered = spatialReader.query(layerId).toRDD
          val filtered = spatialReader.query(layerId).where(Intersects(tileBounds)).toRDD

          val queryKeyBounds = resolveQuery(layerId, query)
          val expected = unfiltered.collect.filter { case (key, value) =>
            queryKeyBounds.includeKey(key)
          }.toMap

          val actual = filtered.collect.toMap

          actual.keys should be (expected.keys)

          for(key <- actual.keys) {
            tilesEqual(actual(key), expected(key))
          }
        }

        it("should be able to combine pairs via Traversable"){
          val tileBounds = GridBounds(915,611,917,616)
          val rdd1 = spatialReader.query(LayerId("ones", zoom)).where(Intersects(tileBounds)).toRDD
          val rdd2 = spatialReader.query(LayerId("ones", zoom)).where(Intersects(tileBounds)).toRDD
          val rdd3 = spatialReader.query(LayerId("ones", zoom)).where(Intersects(tileBounds)).toRDD

          val expected = rdd1.combinePairs(Seq(rdd2, rdd3)){ pairs: Traversable[(SpatialKey, Tile)] =>
            pairs.toSeq.reverse.head
          }

          val actual = Seq(rdd1, rdd2, rdd3).combinePairs { pairs: Traversable[(SpatialKey, Tile)] =>
            pairs.toSeq.reverse.head
          }

          rastersEqual(expected, actual)
        }

        it("should load one tile") {
          val key = SpatialKey(915,612)

          val unfiltered = spatialReader.query(LayerId("ones", zoom)).toRDD
          val (_, expected) = unfiltered.collect.filter { case (k, _) => k == key }.head

          val tileReader = HadoopTileReader[SpatialKey, Tile](attributeStore, layerId)
          val actual = tileReader(key)

          tilesEqual(actual, expected)
        }

        it("should allow filtering files in hadoopGeoTiffRDD") {
          val tilesDir = new Path(localFS.getWorkingDirectory,
                                  "../raster-test/data/one-month-tiles/")
          val source = sc.hadoopGeoTiffRDD(tilesDir)

          // Raises exception if the bogus file isn't properly filtered out
          Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
        }

        it("should allow overriding tiff file extensions in hadoopGeoTiffRDD") {
          val tilesDir = new Path(localFS.getWorkingDirectory,
                                  "../raster-test/data/one-month-tiles-tiff/")
          val source = sc.hadoopGeoTiffRDD(tilesDir, ".tiff")

          // Raises exception if the ".tiff" extension override isn't provided
          Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
        }
      }

      it("should have written and read coordinate space time tiles") {
        CoordinateSpaceTime.collect.map { case (key, tile) =>
          val value = {
            val c = key.spatialKey.col * 1000.0
            val r = key.spatialKey.row
            val t = (key.temporalKey.time.getYear - 2010) / 1000.0

            c + r + t
          }

          tile.foreachDouble { z => z should be (value.toDouble +- 0.0009999999999) }
        }
      }


      it("should have ran") {
        ran should be (true)
      }

      it("ZCurveKeyIndexMethod.byYear") {
        val coordST = CoordinateSpaceTime
        spaceTimeWriter
          .write(LayerId("coordinates", 10), coordST)
        rastersEqual(spaceTimeReader.query(LayerId("coordinates", 10)).toRDD, coordST)
      }

      it("ZCurveKeyIndexMethod.by(DateTime => Int)") {
        val coordST = CoordinateSpaceTime
        val tIndex = (x: DateTime) =>  if (x < DateTime.now) 1 else 0

        spaceTimeWriter
          .write(LayerId("coordinates", 10), coordST)

        val rdd = spaceTimeReader.query(LayerId("coordinates", 10)).toRDD
        rastersEqual(rdd, coordST)
      }

      it("HilbertKeyIndexMethod with min, max, and resolution") {
        val coordST = CoordinateSpaceTime
        val now = DateTime.now

        HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](catalogPath, HilbertKeyIndexMethod(now - 20.years, now, 4))
          .write(LayerId("coordinates", 10), coordST)
        rastersEqual(spaceTimeReader.query(LayerId("coordinates", 10)).toRDD, coordST)
      }

      it("HilbertKeyIndexMethod with only resolution") {
        val coordST = CoordinateSpaceTime

        HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](catalogPath, HilbertKeyIndexMethod(2))
          .write(LayerId("coordinates", 10), coordST)
        rastersEqual(spaceTimeReader.query(LayerId("coordinates", 10)).toRDD, coordST)
      }
    }
  }
}
