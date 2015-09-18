package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.spark.tiling.{FloatingLayoutScheme, ZoomedLayoutScheme}
import org.apache.spark.storage.StorageLevel
import org.rogach.scallop._
import reflect.runtime.universe._

class EtlConf(args: Seq[String]) extends ScallopConf(args){
  val input       = opt[String]("input", required = true,
                      descr = "name of input module (ex: s3, hadoop)")
  val format       = opt[String]("format", required = true,
                      descr = "input file format (ex: geotiff)")
  val cache        = opt[StorageLevel]("cache",
                      descr = "spark rdd storage level to be used for caching (default: MEMORY_AND_DISK_SER)",
                      default = Some(StorageLevel.MEMORY_AND_DISK_SER))
  val layoutScheme = opt[LayoutSchemeProvider]("layoutScheme",
                      descr = "layout scheme to use for tiling: (tms, floating)",
                      default = Some((crs, tileSize) => ZoomedLayoutScheme(crs, tileSize)))(EtlConf.layoutSchemeConverter)
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
  val clobber      = opt[Boolean]("clobber",
                      descr = "clobber layer on save (default: false)",
                      default = Some(false))
  val pyramid      = opt[Boolean]("pyramid",
                      descr = "pyramid layer on save (default: false)",
                      default = Some(false))

  val histogram    = opt[Boolean]("histogram",
                      descr = "compute and store histogram on save (default: false)",
                      default = Some(false))

  val outputProps  = props[String]('O',
                      descr = "parameters for output module")

  implicit def crsConverter: ValueConverter[CRS] = singleArgConverter[CRS](CRS.fromName)
  implicit def storageLevelConvert: ValueConverter[StorageLevel] = singleArgConverter[StorageLevel](StorageLevel.fromString)
}
object EtlConf {
 val layoutSchemeConverter = new ValueConverter[LayoutSchemeProvider] {
   val validNames = List ("tms", "floating")

   def parse(s : List[(String, List[String])]) = s match {
     case (_, schemeName :: Nil) :: Nil  if validNames contains schemeName =>
        Right(Some(schemeName match {
          case "floating" =>
            (crs: CRS, tileSize: Int) => FloatingLayoutScheme(tileSize)
          case "tms" =>
            (crs: CRS, tileSize: Int) => ZoomedLayoutScheme(crs, tileSize)
        }))
     case _ =>
       Left("wrong arguments format")
   }

   val tag = typeTag[LayoutSchemeProvider]
   val argType = org.rogach.scallop.ArgType.LIST
 }
}

