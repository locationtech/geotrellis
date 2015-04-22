package geotrellis.spark.io.s3

import org.scalatest._
import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles._
import geotrellis.raster.GridBounds
import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentialsProvider}

class S3RasterCatalogSpec extends FunSpec
  with TestFiles
  with Matchers
  with OnlyIfCanRunSpark
{
  describe("S3 Raster Catalog") {
    ifCanRunSpark {      
      val rdd = AllOnesTestFile
      val id = LayerId("ones", 2)

      val catalog = S3RasterCatalog("climate-catalog", "catalog", () => new MockS3Client )

      it("should save to s3"){
        catalog.writer[SpatialKey](ZCurveKeyIndexMethod, "subdir").write(id, AllOnesTestFile)
      }

      it("should load from s3"){
        val rdd = catalog.reader[SpatialKey].read(id)
        info(s"RDD count: ${rdd.count}")
        info(rdd.metaData.gridBounds.toString)
      }

      it("should be able to filter?"){
        val rdd = catalog.reader[SpatialKey].read(id, 
          FilterSet(SpaceFilter(GridBounds(2, 2, 3, 3))))
        info(s"RDD count: ${rdd.count}")
        rdd.count should equal (4)
        rdd.collect.foreach { case (key, tile) =>
          info(key.toString)
        }
      }
    }
  }
}