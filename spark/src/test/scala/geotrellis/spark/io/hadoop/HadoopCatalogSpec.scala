package geotrellis.spark.io.hadoop

import java.io.IOException

import geotrellis.raster._

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.raster.op.local._
import geotrellis.proj4.LatLng
import org.scalatest._
import org.apache.hadoop.fs.Path

class HadoopCatalogSpec extends FunSpec
with Matchers
with TestEnvironment
with OnlyIfCanRunSpark
{

  describe("Hadoop Catalog") {
    ifCanRunSpark {
      val catalogPath = new Path(inputHome, ("catalog-spec"))
      val fs = catalogPath.getFileSystem(sc.hadoopConfiguration)
      HdfsUtils.deletePath(catalogPath, sc.hadoopConfiguration)
      val catalog: HadoopCatalog = HadoopCatalog(sc, catalogPath)

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sc.hadoopGeoTiffRDD(allOnes)
      val layoutScheme = ZoomedLayoutScheme(512)

      val (level, onesRdd) = Ingest(source, LatLng, layoutScheme)


      it("should succeed saving with default Props"){
        catalog.save(LayerId("ones", level.zoom), onesRdd)
        assert(fs.exists(new Path(catalogPath, "ones")))
      }

      it("should succeed saving with single path Props"){
        catalog.save(LayerId("ones", level.zoom), "sub1", onesRdd)
        assert(fs.exists(new Path(catalogPath, "sub1/ones")))
      }

      it("should succeed saving with double path Props"){
        catalog.save(LayerId("ones", level.zoom), "sub1/sub2", onesRdd)
        assert(fs.exists(new Path(catalogPath, "sub1/sub2/ones")))
      }

      it("should load out saved tiles"){
        catalog.load[SpatialKey](LayerId("ones", 10)).count should be > 0l
      }

      it("should succeed loading with single path Props"){
        catalog.load[SpatialKey](LayerId("ones", level.zoom), "sub1").count should be > 0l
      }

      it("should succeed loading with double path Props"){
        catalog.load[SpatialKey](LayerId("ones", level.zoom), "sub1/sub2").count should be > 0l
      }


      it("should load out saved tiles, but only for the right zoom"){
        intercept[LayerNotFoundError] {
          catalog.load[SpatialKey](LayerId("ones", 9)).count()
        }
      }

      it("fetch a TileExtent from catalog"){
        val tileBounds = GridBounds(915,611,917,616)
        val filters = new FilterSet[SpatialKey] withFilter SpaceFilter(tileBounds)
        val rdd1 = catalog.load[SpatialKey](LayerId("ones", 10), filters)
        val rdd2 = catalog.load[SpatialKey](LayerId("ones", 10), filters)
        val out = rdd1.combinePairs(rdd2){case (tms1, tms2) =>
          require(tms1.id == tms2.id)
          val res = tms1.tile.localAdd(tms2.tile)
          (tms1.id, res)
        }

        val tile = out.first.tile
        tile.get(497,511) should be (2)
      }

      it("should find default params based on key") {
        val defaultParams = HadoopCatalog.BaseParams.withKeyParams[SpatialKey]("spatial-layers")
        val cat: HadoopCatalog = HadoopCatalog(sc, catalogPath, defaultParams)
        cat.save(LayerId("spatial-ones", level.zoom), onesRdd)
        assert(fs.exists(new Path(catalogPath, "spatial-layers/spatial-ones")))
      }

      it("should find default params based on LayerId") {
        val defaultParams = HadoopCatalog.BaseParams
          .withKeyParams[SpatialKey]("spatial-layers")
          .withLayerParams[SpatialKey]{ case LayerId(name, zoom) if name.startsWith("ones") =>  "special" }

        //LayerParams should take priority
        val cat: HadoopCatalog = HadoopCatalog(sc, catalogPath, defaultParams)
        cat.save(LayerId("onesSpecial", level.zoom), onesRdd)
        assert(fs.exists(new Path(catalogPath, "special/onesSpecial")))
      }
    }
  }
}
