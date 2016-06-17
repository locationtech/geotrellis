package geotrellis.spark.etl.config.json

import geotrellis.spark.io._
import geotrellis.vector.io._
import geotrellis.raster.{CellSize, CellType}
import geotrellis.raster.resample._
import geotrellis.spark.etl.{BufferedReproject, PerTileReproject, ReprojectMethod}
import geotrellis.spark.etl.config.backend.{BackendInputType, _}
import geotrellis.spark.etl.config.dataset._
import org.apache.spark.storage.StorageLevel
import spray.json._
import spray.json.DefaultJsonProtocol._

object ConfigFormats extends ConfigFormats

trait ConfigFormats {
  implicit object CellTypeReader extends RootJsonFormat[CellType] {
    def write(ct: CellType): JsValue = ???
    def read(value: JsValue): CellType =
      value match {
        case JsString(ctype) => CellType.fromString(ctype)
        case _ =>
          throw new DeserializationException("CellType must be a valid string.")
      }
  }

  implicit object StorageLevelReader extends RootJsonFormat[StorageLevel] {
    def write(sl: StorageLevel): JsValue = ???
    def read(value: JsValue): StorageLevel =
      value match {
        case JsString(storageLevel) => StorageLevel.fromString(storageLevel)
        case _ =>
          throw new DeserializationException("StorageLevel must be a valid string.")
      }
  }

  implicit object BackendInputTypeReader extends RootJsonFormat[BackendInputType] {
    def write(bit: BackendInputType): JsValue = ???
    def read(value: JsValue): BackendInputType =
      value match {
        case JsString(backend) => BackendType.fromNameInput(backend)
        case _ =>
          throw new DeserializationException("BackendInputType must be a valid string.")
      }
  }

  implicit object BackendTypeReader extends RootJsonFormat[BackendType] {
    def write(bt: BackendType): JsValue = ???
    def read(value: JsValue): BackendType =
      value match {
        case JsString(backend) => BackendType.fromName(backend)
        case _ =>
          throw new DeserializationException("BackendType must be a valid string.")
      }
  }

  implicit object PointResampleMethodTypeReader extends RootJsonFormat[PointResampleMethod] {
    def write(prm: PointResampleMethod): JsValue = ???
    def read(value: JsValue): PointResampleMethod =
      value match {
        case JsString(backend) => backend match {
          case "nearest-neighbor"  => NearestNeighbor
          case "bilinear"          => Bilinear
          case "cubic-convolution" => CubicConvolution
          case "cubic-spline"      => CubicSpline
          case "lanczos"           => Lanczos
        }
        case _ =>
          throw new DeserializationException("PointResampleMethod must be a valid string.")
      }
  }

  implicit object CellSizeReader extends RootJsonFormat[CellSize] {
    def write(cs: CellSize): JsValue = ???
    def read(value: JsValue): CellSize =
      value.asJsObject.getFields("width", "height") match {
        case Seq(JsNumber(width), JsNumber(height)) => CellSize(width.toInt, height.toInt)
        case _ =>
          throw new DeserializationException("BackendType must be a valid object.")
      }
  }

  implicit object ReprojectMethodReader extends RootJsonFormat[ReprojectMethod] {
    def write(rm: ReprojectMethod): JsValue = ???
    def read(value: JsValue): ReprojectMethod =
      value match {
        case JsString(backend) => backend match {
          case "buffered" => BufferedReproject
          case "per-tile" => PerTileReproject
          case _ =>
            throw new DeserializationException("ReprojectMethod must be a valid string.")
        }
        case _ =>
          throw new DeserializationException("ReprojectMethod must be a valid string.")
      }
  }

  implicit val accumuloBackendFromat    = jsonFormat6(Accumulo)
  implicit val cassandraBackendFromat   = jsonFormat9(Cassandra)
  implicit val hadoopBackendFromat      = jsonFormat1(Hadoop)
  implicit val s3BackendFromat          = jsonFormat1(S3)
  implicit val credentialsBackendFromat = jsonFormat4(Credentials)
  implicit val ingestPathFormat         = jsonFormat2(IngestPath)
  implicit val ingestKeyIndexFromat = jsonFormat4(IngestKeyIndexMethod)
  implicit val ingestTypeFromat = jsonFormat5(IngestType)
  implicit val ingestOptions = jsonFormat11(IngestOptions)
  implicit val configFromat = jsonFormat5(Config.apply)
}
