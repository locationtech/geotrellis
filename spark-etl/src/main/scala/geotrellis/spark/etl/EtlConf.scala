package geotrellis.spark.etl

import geotrellis.proj4.CRS
import org.apache.spark.storage.StorageLevel
import org.rogach.scallop._

class EtlConf(args: Seq[String]) extends ScallopConf(args){
  val input       = opt[String]("input", required = true,
                      descr = "name of input module (ex: s3, hadoop)")
  val format       = opt[String]("format", required = true,
                      descr = "input file format (ex: geotiff)")
  val cache        = opt[StorageLevel]("cache",
                      descr = "spark rdd storage level to be used for caching (default: MEMORY_AND_DISK_SER)",
                      default = Some(StorageLevel.MEMORY_AND_DISK_SER))
  val inputProps   = props[String]('I',
                      descr = "parameters for input module")
  val output       = opt[String]("output", required = true,
                      descr ="name of output module (ex: s3, hadoop, accumulo")
  val layerName    = opt[String]("layer", required = true,
                      descr = "Name of the input layer")
  val crs          = opt[CRS]("crs", required = true,
                      descr = "target CRS for the layer (ex: EPSG:3857")
  val tileSize     = opt[Int]("tileSize",
                      descr = "Tile size in pixels (default: 256)",
                      default = Some(256))

  val outputProps  = props[String]('O',
                      descr = "parameters for output module")
  val clobber      = opt[Boolean]("clobber",
                      descr = "clobber layer on save (default: false)",
                      default = Some(false))
  val pyramid      = opt[Boolean]("pyramid",
                      descr = "pyramid layer on save (default: false)",
                      default = Some(false))

  implicit def crsConverter: ValueConverter[CRS] = singleArgConverter[CRS](CRS.fromName)
  implicit def storageLevelConvert: ValueConverter[StorageLevel] = singleArgConverter[StorageLevel](StorageLevel.fromString)
}