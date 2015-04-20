package geotrellis.spark.io.s3

import org.scalatest._
import geotrellis.spark._
import geotrellis.spark.testfiles._
import geotrellis.raster.GridBounds

class S3RasterCatalogSpec extends FunSpec
  with TestFiles
  with Matchers
  with OnlyIfCanRunSpark
{
  describe("S3 Raster Catalog") {
    ifCanRunSpark {
      val rdd = AllOnesTestFile
      val id = LayerId("ones", 2)

      it("should save to s3"){
        val catalog = S3RasterCatalog("climate-catalog", "catalog")  
        catalog.writer[SpatialKey]("subdir").write(id, AllOnesTestFile)
      }

      it("should load from s3"){
        val catalog = S3RasterCatalog("climate-catalog", "catalog")
        val rdd = catalog.reader[SpatialKey].read(id)
        info(s"RDD count: ${rdd.count}")
        info(rdd.metaData.gridBounds.toString)
      }

      it("should be able to filter?"){
        val catalog = S3RasterCatalog("climate-catalog", "catalog")
        val rdd = catalog.reader[SpatialKey].read(id, 
          FilterSet(SpaceFilter(GridBounds(227, 153, 229, 154))))
        info(s"RDD count: ${rdd.count}")
        rdd.collect.foreach { case (key, tile) =>
          info(key.toString)
        }
      }
    }
  }
}