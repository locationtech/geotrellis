package geotrellis.spark.etl.config.json

import geotrellis.spark.io._
import geotrellis.vector.io._
import geotrellis.raster.{CellSize, CellType}
import geotrellis.raster.resample._
import geotrellis.spark.etl.config._

import org.apache.spark.storage.StorageLevel
import spray.json._
import spray.json.DefaultJsonProtocol._

trait ConfigFormats {
  implicit object CellTypeReader extends RootJsonFormat[CellType] {
    def write(ct: CellType): JsValue = ct.name.toJson
    def read(value: JsValue): CellType =
      value match {
        case JsString(ctype) => CellType.fromString(ctype)
        case _ =>
          throw new DeserializationException("CellType must be a valid string.")
      }
  }

  implicit object StorageLevelReader extends RootJsonFormat[StorageLevel] {
    def write(sl: StorageLevel): JsValue = sl match {
      case StorageLevel.NONE => "NONE".toJson
      case StorageLevel.DISK_ONLY => "DISK_ONLY".toJson
      case StorageLevel.MEMORY_ONLY => "MEMORY_ONLY".toJson
      case StorageLevel.MEMORY_ONLY_2 => "MEMORY_ONLY_2".toJson
      case StorageLevel.MEMORY_ONLY_SER => "MEMORY_ONLY_SER".toJson
      case StorageLevel.MEMORY_ONLY_SER_2 => "MEMORY_ONLY_SER_2".toJson
      case StorageLevel.MEMORY_AND_DISK => "MEMORY_ONLY_DISK".toJson
      case StorageLevel.MEMORY_AND_DISK_2 => "MEMORY_ONLY_DISK_2".toJson
      case StorageLevel.MEMORY_AND_DISK_SER => "MEMORY_ONLY_DISK_SER".toJson
      case StorageLevel.MEMORY_AND_DISK_SER_2 => "MEMORY_ONLY_DISK_SER_2".toJson
      case StorageLevel.OFF_HEAP => "OFF_HEAP".toJson
      case _ => throw new IllegalArgumentException(s"Invalid StorageLevel: $sl")
    }
    def read(value: JsValue): StorageLevel =
      value match {
        case JsString(storageLevel) => StorageLevel.fromString(storageLevel)
        case _ =>
          throw new DeserializationException("StorageLevel must be a valid string.")
      }
  }

  implicit object BackendInputTypeReader extends RootJsonFormat[BackendInputType] {
    def write(bit: BackendInputType): JsValue = bit.name.toJson
    def read(value: JsValue): BackendInputType =
      value match {
        case JsString(backend) => BackendInputType.fromString(backend)
        case _ =>
          throw new DeserializationException("BackendInputType must be a valid string.")
      }
  }

  implicit object BackendTypeReader extends RootJsonFormat[BackendType] {
    def write(bt: BackendType): JsValue = bt.name.toJson
    def read(value: JsValue): BackendType =
      value match {
        case JsString(backend) => BackendType.fromString(backend)
        case _ =>
          throw new DeserializationException("BackendType must be a valid string.")
      }
  }

  implicit object PointResampleMethodTypeReader extends RootJsonFormat[PointResampleMethod] {
    def write(prm: PointResampleMethod): JsValue = prm match {
      case NearestNeighbor  => "nearest-neighbor".toJson
      case Bilinear         => "bilinear".toJson
      case CubicConvolution => "cubic-convolution".toJson
      case CubicSpline      => "cubic-spline".toJson
      case Lanczos          => "lanczos".toJson
    }
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
    def write(cs: CellSize): JsValue = JsObject(
      "width"  -> cs.width.toJson,
      "height" -> cs.height.toJson
    )
    def read(value: JsValue): CellSize =
      value.asJsObject.getFields("width", "height") match {
        case Seq(JsNumber(width), JsNumber(height)) => CellSize(width.toInt, height.toInt)
        case _ =>
          throw new DeserializationException("BackendType must be a valid object.")
      }
  }

  implicit object ReprojectMethodReader extends RootJsonFormat[ReprojectMethod] {
    def write(rm: ReprojectMethod): JsValue = rm.name.toJson
    def read(value: JsValue): ReprojectMethod =
      value match {
        case JsString(backend) => ReprojectMethod.fromString(backend)
        case _ =>
          throw new DeserializationException("ReprojectMethod must be a valid string.")
      }
  }

  implicit val accumuloBackendFromat    = jsonFormat6(Accumulo)
  implicit val cassandraBackendFromat   = jsonFormat9(Cassandra)
  implicit val hadoopBackendFromat      = jsonFormat1(Hadoop)
  implicit val s3BackendFromat          = jsonFormat2(S3)
  implicit val credentialsBackendFromat = jsonFormat4(Credentials)
  implicit val ingestPathFormat         = jsonFormat2(IngestPath)
  implicit val ingestKeyIndexFromat     = jsonFormat4(IngestKeyIndexMethod)
  implicit val ingestTypeFromat         = jsonFormat5(IngestType)
  implicit val ingestOptions            = jsonFormat14(IngestOptions)
  implicit val configFromat             = jsonFormat5(Config.apply)
}
