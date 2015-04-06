package geotrellis.spark.io.hadoop

import java.io.IOException

import geotrellis.raster._
import geotrellis.vector._ 

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.raster.op.local._
import geotrellis.proj4.LatLng
import org.scalatest._
import org.apache.hadoop.fs.Path

class HadoopRasterCatalogSpec extends FunSpec
    with Matchers
    with RasterRDDMatchers
    with TestEnvironment
    with OnlyIfCanRunSpark
{

  describe("HadoopRasterCatalog with SpatialKey Rasters") {
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
          catalog.writer[SpatialKey].write(LayerId("ones", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "ones")))
        }

        it("should succeed saving with single path Props"){
          catalog.writer[SpatialKey]("sub1").write(LayerId("ones", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "sub1/ones")))
        }

        it("should succeed saving with double path Props"){
          catalog.writer[SpatialKey]("sub1/sub2").write(LayerId("ones", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "sub1/sub2/ones")))
        }

        it("should load out saved tiles"){
          catalog.reader[SpatialKey].read(LayerId("ones", 10)).count should be > 0l
        }

        it("should succeed loading with single path Props"){
          catalog.reader[SpatialKey].read(LayerId("ones", level.zoom)).count should be > 0l
        }

        it("should succeed loading with double path Props"){
          catalog.reader[SpatialKey].read(LayerId("ones", level.zoom)).count should be > 0l
        }

        it("should load out saved tiles, but only for the right zoom"){
          intercept[LayerNotFoundError] {
            catalog.reader[SpatialKey].read(LayerId("ones", 9)).count()
          }
        }

        /* TODO: Support filters in Hadoop? 
        it("fetch a TileExtent from catalog"){
          val tileBounds = GridBounds(915,611,917,616)
          val filters = new FilterSet[SpatialKey] withFilter SpaceFilter(tileBounds)
          val rdd1 = catalog.reader[SpatialKey].read(LayerId("ones", 10), filters)
          val rdd2 = catalog.reader[SpatialKey].read(LayerId("ones", 10), filters)
          val out = rdd1.combinePairs(rdd2){case (tms1, tms2) =>
            require(tms1.id == tms2.id)
            val res = tms1.tile.localAdd(tms2.tile)
            (tms1.id, res)
          }

          val tile = out.first.tile
          tile.get(497,511) should be (2)
        }

        it("should be able to combine pairs via Traversable"){
          val tileBounds = GridBounds(915,611,917,616)
          val filters = new FilterSet[SpatialKey] withFilter SpaceFilter(tileBounds)
          val rdd1 = catalog.reader[SpatialKey].read(LayerId("ones", 10), filters)
          val rdd2 = catalog.reader[SpatialKey].read(LayerId("ones", 10), filters)
          val rdd3 = catalog.reader[SpatialKey].read(LayerId("ones", 10), filters)

          val expected = rdd1.combinePairs(Seq(rdd2, rdd3)){ pairs: Traversable[(SpatialKey, Tile)] =>
            pairs.toSeq.reverse.head
          }

          val actual = Seq(rdd1, rdd2, rdd3).combinePairs { pairs: Traversable[(SpatialKey, Tile)] =>
            pairs.toSeq.reverse.head
          }

          rastersEqual(expected, actual)
        }
         */

        it("should find default params based on key") {
          val defaultParams = HadoopRasterCatalog.BaseParams.withKeyParams[SpatialKey]("spatial-layers")
          val cat = HadoopRasterCatalog(catalogPath, defaultParams)
          cat.writer[SpatialKey].write(LayerId("spatial-ones", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "spatial-layers/spatial-ones")))
        }

        it("should find default params based on LayerId") {
          val defaultParams = HadoopRasterCatalog.BaseParams
            .withKeyParams[SpatialKey]("spatial-layers")
            .withLayerParams[SpatialKey]{ case LayerId(name, zoom) if name.startsWith("ones") =>  "special" }

          //LayerParams should take priority
          val cat = HadoopRasterCatalog(catalogPath, defaultParams)
          cat.writer[SpatialKey].write(LayerId("onesSpecial", level.zoom), onesRdd)
          assert(fs.exists(new Path(catalogPath, "special/onesSpecial")))
        }

        it("should allow filtering files in hadoopGeoTiffRDD") {
          val tilesDir = new Path(localFS.getWorkingDirectory, "../raster-test/data/one-month-tiles/")
          val source = sc.hadoopGeoTiffRDD(tilesDir)

          // Raises exception if the bogus file isn't properly filtered out
          Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
        }

        it("should allow overriding tiff file extensions in hadoopGeoTiffRDD") {
          val tilesDir = new Path(localFS.getWorkingDirectory, "../raster-test/data/one-month-tiles-tiff/")
          val source = sc.hadoopGeoTiffRDD(tilesDir, ".tiff")

          // Raises exception if the ".tiff" extension override isn't provided
          Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
        }
      }

      it("should have ran") {
        ran should be (true)
      }
    }
  }

  // describe("HadoopRasterCatalog with SpaceTimeKey Rasters") {
  //   ifCanRunSpark {
  //     val catalogPath = new Path(inputHome, ("catalog-spec"))
  //     val fs = catalogPath.getFileSystem(sc.hadoopConfiguration)
  //     HdfsUtils.deletePath(catalogPath, sc.hadoopConfiguration)
  //     val catalog = HadoopRasterCatalog(catalogPath)

  //     val onesRdd = AllOnesSpaceTime

  //     Ingest[ProjectedExtent, SpaceTimeKey](source, LatLng, layoutScheme) { (onesRdd, level) => 

  //       it("should succeed saving with default Props"){
  //         catalog.writer[SpaceTimeKey].write(LayerId("ones", level.zoom), onesRdd)
  //         assert(fs.exists(new Path(catalogPath, "ones")))
  //       }

  //       it("should succeed saving with single path Props"){
  //         catalog.writer[SpaceTimeKey]("sub1").write(LayerId("ones", level.zoom), onesRdd)
  //         assert(fs.exists(new Path(catalogPath, "sub1/ones")))
  //       }

  //       it("should succeed saving with double path Props"){
  //         catalog.writer[SpaceTimeKey]("sub1/sub2").write(LayerId("ones", level.zoom), onesRdd)
  //         assert(fs.exists(new Path(catalogPath, "sub1/sub2/ones")))
  //       }

  //       it("should load out saved tiles"){
  //         catalog.reader[SpaceTimeKey].read(LayerId("ones", 10)).count should be > 0l
  //       }

  //       it("should succeed loading with single path Props"){
  //         catalog.reader[SpaceTimeKey].read(LayerId("ones", level.zoom)).count should be > 0l
  //       }

  //       it("should succeed loading with double path Props"){
  //         catalog.reader[SpaceTimeKey].read(LayerId("ones", level.zoom)).count should be > 0l
  //       }

  //       it("should load out saved tiles, but only for the right zoom"){
  //         intercept[LayerNotFoundError] {
  //           catalog.reader[SpaceTimeKey].read(LayerId("ones", 9)).count()
  //         }
  //       }

  //       /* TODO: Support filters in Hadoop? 
  //       it("fetch a TileExtent from catalog"){
  //         val tileBounds = GridBounds(915,611,917,616)
  //         val filters = new FilterSet[SpaceTimeKey] withFilter SpaceFilter(tileBounds)
  //         val rdd1 = catalog.reader[SpaceTimeKey].read(LayerId("ones", 10), filters)
  //         val rdd2 = catalog.reader[SpaceTimeKey].read(LayerId("ones", 10), filters)
  //         val out = rdd1.combinePairs(rdd2){case (tms1, tms2) =>
  //           require(tms1.id == tms2.id)
  //           val res = tms1.tile.localAdd(tms2.tile)
  //           (tms1.id, res)
  //         }

  //         val tile = out.first.tile
  //         tile.get(497,511) should be (2)
  //       }

  //       it("should be able to combine pairs via Traversable"){
  //         val tileBounds = GridBounds(915,611,917,616)
  //         val filters = new FilterSet[SpaceTimeKey] withFilter SpaceFilter(tileBounds)
  //         val rdd1 = catalog.reader[SpaceTimeKey].read(LayerId("ones", 10), filters)
  //         val rdd2 = catalog.reader[SpaceTimeKey].read(LayerId("ones", 10), filters)
  //         val rdd3 = catalog.reader[SpaceTimeKey].read(LayerId("ones", 10), filters)

  //         val expected = rdd1.combinePairs(Seq(rdd2, rdd3)){ pairs: Traversable[(SpaceTimeKey, Tile)] =>
  //           pairs.toSeq.reverse.head
  //         }

  //         val actual = Seq(rdd1, rdd2, rdd3).combinePairs { pairs: Traversable[(SpaceTimeKey, Tile)] =>
  //           pairs.toSeq.reverse.head
  //         }

  //         rastersEqual(expected, actual)
  //       }
  //        */

  //       it("should find default params based on key") {
  //         val defaultParams = HadoopRasterCatalog.BaseParams.withKeyParams[SpaceTimeKey]("spatial-layers")
  //         val cat = HadoopRasterCatalog(catalogPath, defaultParams)
  //         cat.writer[SpaceTimeKey].write(LayerId("spatial-ones", level.zoom), onesRdd)
  //         assert(fs.exists(new Path(catalogPath, "spatial-layers/spatial-ones")))
  //       }

  //       it("should find default params based on LayerId") {
  //         val defaultParams = HadoopRasterCatalog.BaseParams
  //           .withKeyParams[SpaceTimeKey]("spatial-layers")
  //           .withLayerParams[SpaceTimeKey]{ case LayerId(name, zoom) if name.startsWith("ones") =>  "special" }

  //         //LayerParams should take priority
  //         val cat = HadoopRasterCatalog(catalogPath, defaultParams)
  //         cat.writer[SpaceTimeKey].write(LayerId("onesSpecial", level.zoom), onesRdd)
  //         assert(fs.exists(new Path(catalogPath, "special/onesSpecial")))
  //       }

  //       it("should allow filtering files in hadoopGeoTiffRDD") {
  //         val tilesDir = new Path(localFS.getWorkingDirectory, "../raster-test/data/one-month-tiles/")
  //         val source = sc.hadoopGeoTiffRDD(tilesDir)

  //         // Raises exception if the bogus file isn't properly filtered out
  //         Ingest[ProjectedExtent, SpaceTimeKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
  //       }

  //       it("should allow overriding tiff file extensions in hadoopGeoTiffRDD") {
  //         val tilesDir = new Path(localFS.getWorkingDirectory, "../raster-test/data/one-month-tiles-tiff/")
  //         val source = sc.hadoopGeoTiffRDD(tilesDir, ".tiff")

  //         // Raises exception if the ".tiff" extension override isn't provided
  //         Ingest[ProjectedExtent, SpaceTimeKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
  //       }
  //     }
  //   }
  //}
}
