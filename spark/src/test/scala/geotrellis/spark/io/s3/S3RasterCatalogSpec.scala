package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles._
import geotrellis.raster.{Tile, GridBounds}
import org.scalatest._

class S3RasterCatalogSpec extends FunSpec
  with TestFiles
  with Matchers
  with OnlyIfCanRunSpark
{
  describe("S3 Raster Catalog") {
    ifCanRunSpark {      
      val rdd = AllOnesTestFile
      val id = LayerId("ones", 10)

      val bucket = "climate-catalog"
      val prefix = "catalog3"
      val attributeStore = new S3AttributeStore(bucket, prefix) {
        override val s3Client = new MockS3Client
      }
      val spatialReader = new S3LayerReader[SpatialKey, Tile, RasterRDD](attributeStore){
        override val getS3Client = () => new MockS3Client
      }
      val spaceTimeReader = new S3LayerReader[SpatialKey, Tile, RasterRDD](attributeStore){
        override val getS3Client = () => new MockS3Client
      }

      it("should save to s3"){
        val writer = new S3LayerWriter[SpatialKey, Tile, RasterRDD](bucket, prefix, ZCurveKeyIndexMethod)(attributeStore){
          override val getS3Client = () => new MockS3Client
        }
        writer.write(id, AllOnesTestFile)
      }

      it("should know when layer exists"){
        spatialReader.attributeStore.layerExists(id) should be (true)
        spatialReader.attributeStore.layerExists(LayerId("nope", 100)) should be (false)
      }

      it("should load from s3"){
        val rdd = spatialReader.query(id).toRDD
//        val rdd: RasterRDD[SpatialKey] = spatialReader.read(id)
        rdd.count should equal (42)
        info(s"RDD count: ${rdd.count}")
        info(rdd.metaData.gridBounds.toString)
      }

      it("should be able to filter?"){
        val rdd = spatialReader
          .query(id)
          .where(Intersects(GridBounds(2, 2, 3, 3)))
          .toRDD
          
        info(s"RDD count: ${rdd.count}")
        rdd.count should equal (4)
        rdd.collect.foreach { case (key, tile) =>
          info(key.toString)
        }
      }

      it("should work when requested tiles are missing"){
        // actual bound: GridBounds(1,1,6,7)
        val rdd = spatialReader
          .query(id)
          .where(Intersects(GridBounds(1,1,10,10)))
          .toRDD
        info(s"GridBounds: ${rdd.metaData.gridBounds}")
        info(s"RDD count: ${rdd.count}")
        rdd.count should equal (42)
      }

      it("should load out saved tiles") {
        val rdd = spatialReader.read(id)
        rdd.count should be > 0l
        rdd.map(_._1).collect().toSet shouldEqual rdd.map(_._1).collect().toSet
      }

      it("should read a spatial tile"){
        val reader = new S3TileReader[SpatialKey, Tile](attributeStore, id) {
          override val s3Client = new MockS3Client
        }
        val tile = reader(SpatialKey(2,2))
        tile.foreach { x=> x should be (1) }
      }

      it("should error on getting a tile that is not there"){
        val reader = new S3TileReader[SpatialKey, Tile](attributeStore, id) {
          override val s3Client = new MockS3Client
        }

        intercept[TileNotFoundError]{
          val tile = reader(SpatialKey(200,200))
        }        
      }
// TODO: these can not be run until RDDQuery becomes the main type used in tests, it will happen when Accumulo and Hadoop undergo their refactor
//      RasterRDDQueryTest.spatialTest.foreach { test =>
//        it(test.name){
//          val rdd = spatialReader.read(id, test.query)
//          info(rdd.metaData.gridBounds.toString)
//          rdd.map(_._1).collect should contain theSameElementsAs test.expected
//        }
//      }
//
//      val spaceId = LayerId("coordinates", 10)
//      it("should save a spacetime layer"){
//        val writer = new RasterRDDWriter[SpaceTimeKey](bucket, prefix, ZCurveKeyIndexMethod.byYear)(attributeStore){
//          override val getS3Client = () => new MockS3Client
//        }
//
//        writer.write(spaceId, CoordinateSpaceTime)
//      }
//
//      it("should load a spacetime layer"){
//        val rdd = spaceTimeReader.query(spaceId).toRDD
//        rdd.count should equal (210)
//        info(s"RDD count: ${rdd.count}")
//        info(rdd.metaData.gridBounds.toString)
//      }
//
//      it("should list all metadata") {
//        val list = spaceTimeReader.attributeStore.readAll[S3LayerMetaData]("metadata")
//        list.foreach(s => info(s.toString))
//      }
//
//      RasterRDDQueryTest.spaceTimeTest.foreach { test =>
//        it(test.name){
//          val rdd = spaceTimeReader.read(test.layerId, test.query)
//          rdd.map(_._1).collect should contain theSameElementsAs test.expected
//        }
//      }

    }
  }
}