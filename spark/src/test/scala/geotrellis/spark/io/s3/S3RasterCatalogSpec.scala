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
      val id = LayerId("ones", 10)

      val catalog = S3RasterCatalog("climate-catalog", "catalog3", () => new MockS3Client )

      it("should save to s3"){
        catalog.writer[SpatialKey](ZCurveKeyIndexMethod).write(id, AllOnesTestFile)
      }

      it("should load from s3"){
        val rdd = catalog.query[SpatialKey](id).toRDD
        rdd.count should equal (42)
        info(s"RDD count: ${rdd.count}")
        info(rdd.metaData.gridBounds.toString)
      }

      it("should be able to filter?"){
        val rdd = catalog
          .query[SpatialKey](id)
          .where(Intersects(GridBounds(2, 2, 3, 3)))
          .toRDD
          
        info(s"RDD count: ${rdd.count}")
        rdd.count should equal (4)
        rdd.collect.foreach { case (key, tile) =>
          info(key.toString)
        }
      }

      it("should read a spatial tile"){
        val reader = catalog.tileReader[SpatialKey](id)

        val tile = reader(SpatialKey(2,2))
        tile.foreach { x=> x should be (1) }
      }

      it("should error on getting a tile that is not there"){
        val reader = catalog.tileReader[SpatialKey](id)

        intercept[RuntimeException]{
          val tile = reader(SpatialKey(200,200))
        }        
      }

      RasterRDDQueryTest.spatialTest.foreach { test =>
        it(test.name){
          val rdd = catalog.read[SpatialKey](id, test.query)
          info(rdd.metaData.gridBounds.toString)
          rdd.map(_._1).collect should contain theSameElementsAs test.expected
        }
      }

      val spaceId = LayerId("coordinates", 10)
      it("should save a spacetime layer"){
        catalog.writer[SpaceTimeKey](ZCurveKeyIndexMethod.byYear, true).write(spaceId, CoordinateSpaceTime)
      }

      it("should load a spacetime layer"){
        val rdd = catalog.query[SpaceTimeKey](spaceId).toRDD
        rdd.count should equal (210)
        info(s"RDD count: ${rdd.count}")
        info(rdd.metaData.gridBounds.toString)
      }

      it("should list all metadata") {
        val list = catalog.attributeStore.readAll[S3LayerMetaData]("metadata")
        list.foreach(s => info(s.toString))
      }

      RasterRDDQueryTest.spaceTimeTest.foreach { test =>
        it(test.name){
          val rdd = catalog.read[SpaceTimeKey](test.layerId, test.query)
          rdd.map(_._1).collect should contain theSameElementsAs test.expected
        }
      }

    }
  }
}