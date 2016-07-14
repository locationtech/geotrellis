package geotrellis.spark.io.hbase

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.spark.tiling._
import geotrellis.proj4._
import geotrellis.spark.util.SparkUtils
import geotrellis.raster.render._
import geotrellis.vector._

import org.apache.spark._

import org.apache.spark.SparkContext
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.scalatest.FunSpec

class Test extends FunSpec {

  val instance = HBaseInstance(Seq("localhost"), "localhost")
  val attributeStore = HBaseAttributeStore(instance)
  //attributeStore.write(LayerId("layerId", 13), "test", "string3")

  implicit val sc = SparkUtils.createLocalSparkContext("local[*]", "test ingest")

  val source = sc.hadoopGeoTiffRDD("/data/tmp-load/")

  val writer = HBaseLayerWriter(instance, "tile_hbase")
  val reader = HBaseLayerReader(instance)

  val layoutScheme = ZoomedLayoutScheme(CRS.fromName("EPSG:3857"))

  /*Ingest[ProjectedExtent, SpatialKey](source, CRS.fromName("EPSG:3857"), layoutScheme, false) { (rdd, zoom) ⇒
    val layerId = LayerId("layerName", zoom)
    writer.write[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId, rdd, ZCurveKeyIndexMethod)
    //attributeStore.write(layerId, "histogram", rdd.histogram)
  }*/

  //writer.write[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](source)

  val rdd = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId("layerName", 8))

  rdd.collect().foreach { case (k, v) => v.renderPng.write(s"/tmp/${k.col}${k.row}l.png") }
  //rdd.take(1).renderPng

  println(attributeStore.layerIds)

  println(attributeStore.layerExists(LayerId("layerId", 12)))

  //println(attributeStore.readAll[String]("test"))
}
