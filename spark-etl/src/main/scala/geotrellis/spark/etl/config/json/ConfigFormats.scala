package geotrellis.spark.etl.config.json

import geotrellis.vector.io._
import geotrellis.raster.{CellSize, CellType}
import geotrellis.raster.resample._
import geotrellis.spark.etl.config._
import geotrellis.vector.Extent

import org.apache.spark.storage.StorageLevel
import spray.json._
import spray.json.DefaultJsonProtocol._

trait ConfigFormats {
  implicit object CellTypeFormat extends RootJsonFormat[CellType] {
    def write(ct: CellType): JsValue = ct.name.toJson
    def read(value: JsValue): CellType =
      value match {
        case JsString(ctype) => CellType.fromString(ctype)
        case _ =>
          throw new DeserializationException("CellType must be a valid string.")
      }
  }

  implicit object StorageLevelFormat extends RootJsonFormat[StorageLevel] {
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

  implicit object BackendInputTypeFormat extends RootJsonFormat[BackendInputType] {
    def write(bit: BackendInputType): JsValue = bit.name.toJson
    def read(value: JsValue): BackendInputType =
      value match {
        case JsString(backend) => BackendInputType.fromString(backend)
        case _ =>
          throw new DeserializationException("BackendInputType must be a valid string.")
      }
  }

  implicit object BackendTypeFormat extends RootJsonFormat[BackendType] {
    def write(bt: BackendType): JsValue = bt.name.toJson
    def read(value: JsValue): BackendType =
      value match {
        case JsString(backend) => BackendType.fromString(backend)
        case _ =>
          throw new DeserializationException("BackendType must be a valid string.")
      }
  }

  implicit object PointResampleMethodTypeFormat extends RootJsonFormat[PointResampleMethod] {
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

  implicit object CellSizeFormat extends RootJsonFormat[CellSize] {
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

  implicit object ReprojectMethodFormat extends RootJsonFormat[ReprojectMethod] {
    def write(rm: ReprojectMethod): JsValue = rm.name.toJson
    def read(value: JsValue): ReprojectMethod =
      value match {
        case JsString(backend) => ReprojectMethod.fromString(backend)
        case _ =>
          throw new DeserializationException("ReprojectMethod must be a valid string.")
      }
  }

  implicit object BackendProfilesReader extends RootJsonReader[Map[String, BackendProfile]] {
    def read(value: JsValue): Map[String, BackendProfile] =
      value.asJsObject.getFields("backend-profiles") match {
        case Seq(bp: JsArray) =>
          bp.elements.map { js: JsValue =>
            js.asJsObject.getFields("name", "type") match {
              case Seq(JsString(n), JsString(t)) => n -> (BackendType.fromString(t) match {
                case HadoopType => js.convertTo[HadoopProfile]
                case S3Type => js.convertTo[S3Profile]
                case AccumuloType => js.convertTo[AccumuloProfile]
                case CassandraType => js.convertTo[CassandraProfile]
              })
              case _ =>
                throw new DeserializationException("BackendProfiles must be a valid json object.")
            }
          }.toMap
      }
  }

  implicit val accumuloProfileFormat  = jsonFormat6(AccumuloProfile)
  implicit val hbaseProfileFormat     = jsonFormat3(HBaseProfile)
  implicit val cassandraProfileFormat = jsonFormat9(CassandraProfile)
  implicit val hadoopProfileFormat    = jsonFormat1(HadoopProfile)
  implicit val s3ProfileFormat        = jsonFormat2(S3Profile)
  implicit val ingestKeyIndexFormat   = jsonFormat4(IngestKeyIndexMethod)

  case class BackendFormat(bp: Map[String, BackendProfile]) extends RootJsonFormat[Backend] {
    def write(b: Backend): JsValue = JsObject(
      "type"    -> b.`type`.name.toJson,
      "path"    -> b.path.toJson,
      "profile" -> b.profile.map(_.name).toJson
    )
    def read(value: JsValue): Backend =
      value match {
        case JsObject(fields) =>
          Backend(
            `type`  = fields("type").convertTo[BackendType],
            path    = fields("path").convertTo[String],
            profile = fields.get("profile").map(_.convertTo[String]).fold(Option.empty[BackendProfile])(bp.get)
          )
        case _ =>
          throw new DeserializationException("Backend must be a valid json object.")
      }
  }

  case class InputFormat(bp: Map[String, BackendProfile]) extends RootJsonFormat[Input] {
    val bf = BackendFormat(bp)
    def write(i: Input): JsValue = JsObject(
      "name"    -> i.name.toJson,
      "format"  -> i.format.toJson,
      "backend" -> bf.write(i.backend),
      "cache"   -> i.cache.toJson,
      "noData"  -> i.noData.toJson
    )
    def read(value: JsValue): Input =
      value match {
        case JsObject(fields) =>
          Input(
            name    = fields("name").convertTo[String],
            format  = fields("format").convertTo[String],
            backend = bf.read(fields("backend")),
            cache   = fields.get("cache").map(_.convertTo[StorageLevel]),
            noData  = fields.get("noData").map(_.convertTo[Double])
          )
        case _ =>
          throw new DeserializationException("Input must be a valid json object.")
      }
  }

  case class InputsFormat(bp: Map[String, BackendProfile]) extends RootJsonFormat[List[Input]] {
    val iformat = InputFormat(bp)
    def write(l: List[Input]): JsValue = l.map(iformat.write).toJson
    def read(value: JsValue): List[Input] =
      value match {
        case JsArray(fields) => fields.toList.map(iformat.read)
        case _ =>
          throw new DeserializationException("Input must be a valid json object.")
      }
  }

  case class OutputFormat(bp: Map[String, BackendProfile]) extends RootJsonFormat[Output] {
    val bf = BackendFormat(bp)
    def write(o: Output): JsValue = JsObject(
      "backend"             -> bf.write(o.backend),
      "resampleMethod"      -> o.resampleMethod.toJson,
      "reprojectMethod"     -> o.reprojectMethod.toJson,
      "keyIndexMethod"      -> o.keyIndexMethod.toJson,
      "tileSize"            -> o.tileSize.toJson,
      "pyramid"             -> o.pyramid.toJson,
      "partitions"          -> o.partitions.toJson,
      "layoutScheme"        -> o.layoutExtent.toJson,
      "layoutExtent"        -> o.layoutScheme.toJson,
      "crs"                 -> o.crs.toJson,
      "resolutionThreshold" -> o.resolutionThreshold.toJson,
      "cellSize"            -> o.cellSize.toJson,
      "cellType"            -> o.cellType.toJson,
      "encoding"            -> o.encoding.toJson,
      "breaks"              -> o.breaks.toJson,
      "maxZoom"             -> o.maxZoom.toJson
    )

    def read(value: JsValue): Output =
      value match {
        case JsObject(fields) =>
          Output(
            backend             = bf.read(fields("backend")),
            resampleMethod      = fields("resampleMethod").convertTo[PointResampleMethod],
            reprojectMethod     = fields("reprojectMethod").convertTo[ReprojectMethod],
            keyIndexMethod      = fields("keyIndexMethod").convertTo[IngestKeyIndexMethod],
            tileSize            = fields.get("tileSize").map(_.convertTo[Int]).fold(256)(identity),
            pyramid             = fields.get("pyramid").map(_.convertTo[Boolean]).fold(true)(identity),
            partitions          = fields.get("partitions").map(_.convertTo[Int]),
            layoutScheme        = fields.get("layoutScheme").map(_.convertTo[String]),
            layoutExtent        = fields.get("layoutExtent").map(_.convertTo[Extent]),
            crs                 = fields.get("crs").map(_.convertTo[String]),
            resolutionThreshold = fields.get("resolutionThreshold").map(_.convertTo[Double]),
            cellSize            = fields.get("cellSize").map(_.convertTo[CellSize]),
            cellType            = fields.get("cellType").map(_.convertTo[CellType]),
            encoding            = fields.get("encoding").map(_.convertTo[String]),
            breaks              = fields.get("breaks").map(_.convertTo[String]),
            maxZoom             = fields.get("maxZoom").map(_.convertTo[Int])

          )
        case _ =>
          throw new DeserializationException("Output must be a valid json object.")
      }
  }
}
