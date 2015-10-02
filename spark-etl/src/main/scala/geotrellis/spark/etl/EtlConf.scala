package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.raster.{CellType, CellSize}
import geotrellis.spark.tiling.{FloatingLayoutScheme, ZoomedLayoutScheme}
import geotrellis.vector.Extent
import org.apache.spark.storage.StorageLevel
import org.rogach.scallop._
import scala.util.{Success, Try, Failure}
import reflect.runtime.universe._

class EtlConf(args: Seq[String]) extends ScallopConf(args){
  import EtlConf._

  val input       = opt[String]("input", required = true,
                      descr = "name of input module (ex: s3, hadoop)")
  val format       = opt[String]("format", required = true,
                      descr = "input file format (ex: geotiff)")
  val cache        = opt[StorageLevel]("cache",
                      descr = "spark rdd storage level to be used for caching (default: MEMORY_AND_DISK_SER)",
                      default = Some(StorageLevel.MEMORY_AND_DISK_SER))
  val layoutScheme = opt[LayoutSchemeProvider]("layoutScheme",
                      descr = "layout scheme to use for tiling: (tms, floating)")(layoutSchemeConverter)

  val layoutExtent = opt[Extent]("layoutExtent",
                      descr = "extent of the layout to be used, provide instead of layoutScheme (format: xmin,ymin,xmax,ymax)"
                      )(extentConverter)

  val cellSize     = opt[CellSize]("cellSize",
                      descr = "cell size of the layout, provide instead of layoutScheme (format: width,height)"
                      )(cellSizeConverter)

  val cellType     = opt[CellType]("cellType",
                      descr = "cell type of the layout (format: bool, int8, float32, ...")(cellTypeConverter)

  conflicts(layoutScheme, List(layoutExtent, cellSize))
  dependsOnAll(layoutExtent, List(cellSize, cellType))

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
  def layoutSchemeConverter = new ValueConverter[LayoutSchemeProvider] {
    val validNames = List ("tms", "floating")

    def parse(s : List[(String, List[String])]) = s match {
     case (_, schemeName :: Nil) :: Nil  if validNames contains schemeName =>
        Right(Some(schemeName match {
          case "floating" =>
            (crs: CRS, tileSize: Int) => FloatingLayoutScheme(tileSize)
          case "tms" =>
            (crs: CRS, tileSize: Int) => ZoomedLayoutScheme(crs, tileSize)
        }))
     case Nil =>
       Right(None)
     case _ =>
       Left("wrong arguments format")
    }

    val tag = typeTag[LayoutSchemeProvider]
    val argType = org.rogach.scallop.ArgType.SINGLE
  }

  def extentConverter = new ValueConverter[Extent] {
    def wrong(msg: String) = Left(s"wrong arguments format: $msg")
    def parse(s : List[(String, List[String])]) = s match {
      case (_, str :: Nil) :: Nil  =>
        Try { Extent.fromString(str) } match {
          case Success(extent) =>
            Right(Some(extent))
          case Failure(e) =>
            wrong(e.getMessage)
        }
      case Nil  =>
        Right(None)
      case _ =>
        wrong("use: xmin,ymin,xmax,ymax")
    }

    val tag = typeTag[Extent]
    val argType = org.rogach.scallop.ArgType.SINGLE
  }

  def cellSizeConverter = new ValueConverter[CellSize] {
    val wrong = Left("wrong arguments format, use: width,height")
    def parse(s : List[(String, List[String])]) = s match {
      case (_, str :: Nil) :: Nil  =>
        Try { CellSize.fromString(str) } match {
          case Success(cs) => Right(Some(cs))
          case Failure(_) => wrong
        }
      case Nil  =>
        Right(None)
      case _ => wrong
    }

    val tag = typeTag[CellSize]
    val argType = org.rogach.scallop.ArgType.SINGLE
  }

  def cellTypeConverter = new ValueConverter[CellType] {
    val wrong = Left("wrong arguments format (ex: bool, int16, float32, ...")
    def parse(s : List[(String, List[String])]) = s match {
      case (_, str :: Nil) :: Nil  =>
        Try { CellType.fromString(str) } match {
          case Success(cs) => Right(Some(cs))
          case Failure(_) => wrong
        }
      case Nil  =>
        Right(None)
      case _ => wrong
    }

    val tag = typeTag[CellType]
    val argType = org.rogach.scallop.ArgType.SINGLE
  }
}

