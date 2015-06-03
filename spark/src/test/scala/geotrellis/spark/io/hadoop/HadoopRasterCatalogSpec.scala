package geotrellis.spark.io.hadoop

import java.io.IOException

import geotrellis.raster._
import geotrellis.vector._

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.tiling._
import geotrellis.raster.op.local._
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
    def resolveQuery[K: JsonFormat: ClassTag](catalog: HadoopRasterCatalog, layerId: LayerId, query: RasterRDDQuery[K]) =
      query(
        catalog.attributeStore.read[HadoopLayerMetaData](layerId, "metadata").rasterMetaData,
        catalog.attributeStore.read[KeyBounds[K]](layerId, "keyBounds"))


    ifCanRunSpark {
      val catalogPath = new Path(inputHome, ("catalog-spec"))
      val fs = catalogPath.getFileSystem(sc.hadoopConfiguration)
      HdfsUtils.deletePath(catalogPath, sc.hadoopConfiguration)
      val catalog = HadoopRasterCatalog(catalogPath)

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sc.hadoopGeoTiffRDD(allOnes)
      val layoutScheme = ZoomedLayoutScheme(512)

      var ran = false

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme) { (onesRdd, level) =>
        ran = true

        it("should succeed saving with default Props"){
          catalog
            .writer[SpatialKey](RowMajorKeyIndexMethod)
            .write(LayerId("ones", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "ones")))
        }

        it("should succeed saving with single path Props"){
          catalog
            .writer[SpatialKey](RowMajorKeyIndexMethod, "sub1")
            .write(LayerId("ones", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "sub1/ones")))
        }

        it("should succeed saving with double path Props"){
          catalog
            .writer[SpatialKey](RowMajorKeyIndexMethod, "sub1/sub2")
            .write(LayerId("ones", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "sub1/sub2/ones")))
        }

        it("should load out saved tiles"){
          val rdd = catalog.query[SpatialKey](LayerId("ones", 10)).toRDD
          rdd.count should be > 0l
        }

        it("should succeed loading with single path Props"){
          catalog.query[SpatialKey](LayerId("ones", level.zoom)).toRDD.count should be > 0l
        }

        it("should succeed loading with double path Props"){
          catalog.query[SpatialKey](LayerId("ones", level.zoom)).toRDD.count should be > 0l
        }

        it("should load out saved tiles, but only for the right zoom"){
          intercept[LayerNotFoundError] {
            catalog.query[SpatialKey](LayerId("ones", 9)).toRDD.count()
          }
        }

        it("should filter out all but 4 tiles") {
          val layerId = LayerId("ones", 10)
          val tileBounds = GridBounds(915,612,917,613)

          val query = new RasterRDDQuery[SpatialKey].where(tileBounds)
          val queryKeyBounds = resolveQuery(catalog, layerId, query)

          val expected = catalog
            .query[SpatialKey](layerId)
            .toRDD
            .collect.filter { case (key, _) =>
              queryKeyBounds.includeKey(key)
            }
          val filteredRdd = catalog
            .query[SpatialKey](LayerId("ones", 10))
            .filter(tileBounds)
            .toRDD

          filteredRdd.count should be (expected.size)
        }


        it("should filter out the correct keys") {
          val layerId = LayerId("ones", 10)
          val tileBounds = GridBounds(915,611,915,613)        
          val unfiltered = catalog.query[SpatialKey](layerId).toRDD
          val filtered = catalog.query[SpatialKey](layerId).filter(tileBounds).toRDD

          val query = new RasterRDDQuery[SpatialKey].where(tileBounds)
          val queryKeyBounds = resolveQuery(catalog, layerId, query)

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
          val layerId = LayerId("ones", 10)
          val tileBounds = GridBounds(915,612,917,613)
          val query = new RasterRDDQuery[SpatialKey].where(tileBounds)
          val unfiltered = catalog.query[SpatialKey](layerId).toRDD
          val filtered = catalog.query[SpatialKey](layerId, query)

          val queryKeyBounds = resolveQuery(catalog, layerId, query)
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
          val rdd1 = catalog.query[SpatialKey](LayerId("ones", 10)).filter(tileBounds).toRDD
          val rdd2 = catalog.query[SpatialKey](LayerId("ones", 10)).filter(tileBounds).toRDD
          val rdd3 = catalog.query[SpatialKey](LayerId("ones", 10)).filter(tileBounds).toRDD

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

          val unfiltered = catalog.query[SpatialKey](LayerId("ones", 10)).toRDD
          val (_, expected) = unfiltered.collect.filter { case (k, _) => k == key }.head


          val getTile = catalog.tileReader[SpatialKey](LayerId("ones", 10))
          val actual = getTile(key)

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
        catalog
          .writer[SpaceTimeKey](ZCurveKeyIndexMethod.byYear)
          .write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.query[SpaceTimeKey](LayerId("coordST", 10)).toRDD, coordST)
      }

      it("ZCurveKeyIndexMethod.by(DateTime => Int)") {
        val coordST = CoordinateSpaceTime
        val tIndex = (x: DateTime) =>  if (x < DateTime.now) 1 else 0

        catalog
          .writer[SpaceTimeKey](ZCurveKeyIndexMethod.by(tIndex))
          .write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.query[SpaceTimeKey](LayerId("coordST", 10)).toRDD, coordST)
      }

      it("HilbertKeyIndexMethod with min, max, and resolution") {
        val coordST = CoordinateSpaceTime
        val now = DateTime.now

        catalog
          .writer[SpaceTimeKey](HilbertKeyIndexMethod(now - 20.years, now, 4))
          .write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.query[SpaceTimeKey](LayerId("coordST", 10)).toRDD, coordST)
      }
      it("HilbertKeyIndexMethod with only resolution") {
        val coordST = CoordinateSpaceTime
        val now = DateTime.now

        catalog
          .writer[SpaceTimeKey](HilbertKeyIndexMethod(2))
          .write(LayerId("coordST", 10), coordST)
        rastersEqual(catalog.query[SpaceTimeKey](LayerId("coordST", 10)).toRDD, coordST)
      }
    }
  }
}
