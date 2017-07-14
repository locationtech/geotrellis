package geotrellis.spark.pipeline.json

import geotrellis.proj4.CRS
import geotrellis.spark.pipeline._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark.tiling._
import geotrellis.vector._

import _root_.io.circe.generic.extras.Configuration
import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.generic.extras.semiauto._
import cats.syntax._
import cats.implicits._

import java.net.URI
import scala.util.Try

object Implicits extends Implicits

trait Implicits {
  implicit val config: Configuration = Configuration.default.withDefaults.withSnakeCaseKeys
  val jsonPrinter: Printer = Printer.spaces2.copy(dropNullKeys = true)

  implicit val uriEncoder: Encoder[URI] =
    Encoder.encodeString.contramap[URI] { _.toString }
  implicit val uriDecoder: Decoder[URI] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(URI.create(str)).leftMap(_ => "URI")
    }

  implicit val crsEncoder: Encoder[CRS] =
    Encoder.encodeString.contramap[CRS] { crs => crs.epsgCode.map { c => s"epsg:$c" }.getOrElse(crs.toProj4String) }

  implicit val crsDecoder: Decoder[CRS] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(Try(CRS.fromName(str)) getOrElse CRS.fromString(str)).leftMap(_ => "CRS")
    }

  implicit val extentEncoder: Encoder[Extent] =
    new Encoder[Extent] {
      final def apply(extent: Extent): Json =
        List(extent.xmin, extent.ymin, extent.xmax, extent.ymax).asJson
    }
  implicit val extentDecoder: Decoder[Extent] =
    Decoder[Json] emap { js =>
      (js.as[List[Double]]: Either[DecodingFailure, List[Double]]).map { case List(xmin, ymin, xmax, ymax) =>
        Extent(xmin, ymin, xmax, ymax)
      }.leftMap(_ => "Extent")
    }

  implicit val tileLayoutEncoder: Encoder[TileLayout] = deriveEncoder
  implicit val tileLayoutDecoder: Decoder[TileLayout] = deriveDecoder

  implicit val layoutDefinitionEncoder: Encoder[LayoutDefinition] = deriveEncoder
  implicit val layoutDefinitionDecoder: Decoder[LayoutDefinition] = deriveDecoder

  implicit val layoutSchemeEncoder: Encoder[LayoutScheme] =
    Encoder.instance {
      case scheme: FloatingLayoutScheme =>
        Json.obj(
          "tileCols" -> scheme.tileCols.asJson,
          "tileRows" -> scheme.tileRows.asJson
        )
      case scheme: ZoomedLayoutScheme =>
        Json.obj(
          "crs" -> scheme.crs.asJson,
          "tileSize" -> scheme.tileSize.asJson,
          "resolutionThreshold" -> scheme.resolutionThreshold.asJson
        )
      case _ => throw new Exception("Can't encode LayoutScheme, consider providing your own circe encoder.")
    }
  implicit val layoutSchemeDecoder: Decoder[LayoutScheme] =
    Decoder.decodeJson.emap { json: Json =>
      ((json.hcursor.downField("tileCols").as[Int] |@| json.hcursor.downField("tileRows").as[Int]) map {
        (tileCols, tileRows) => FloatingLayoutScheme(tileCols, tileRows)
      } match {
        case right @ Right(_) => right
        case Left(_) =>
          (json.hcursor.downField("crs").as[CRS] |@| json.hcursor.downField("tileSize").as[Int] |@| json.hcursor.downField("resolutionThreshold").as[Double]) map {
            (crs, tileSize, resolutionThreshold) => ZoomedLayoutScheme(crs, tileSize, resolutionThreshold)
          }
      }).leftMap(_ => "LayoutScheme")
    }

  implicit val layoutSchemeOrLayoutDefinitionEncoder: Encoder[Either[LayoutScheme, LayoutDefinition]] =
    Encoder.instance(_.bifoldMap(_.asJson, _.asJson))
  implicit val layoutSchemeOrLayoutDefinitionDecoder: Decoder[Either[LayoutScheme, LayoutDefinition]] =
    Decoder.decodeJson.emap { json: Json =>
      Either.catchNonFatal(layoutDefinitionDecoder.decodeJson(json) match {
        case Right(v) => Right[LayoutScheme, LayoutDefinition](v)
        case Left(_) => layoutSchemeDecoder.decodeJson(json) match {
          case Right(v) => Left[LayoutScheme, LayoutDefinition](v)
          case Left(_) =>
            throw new Exception(s"Consider using a custom layoutSchemeOrLayoutDefinitionDecoder decoder, can't decode $json")
        }
      }).leftMap(_ => "layoutSchemeOrLayoutDefinition")
    }

  implicit val pointResampleMethodEncoder: Encoder[PointResampleMethod] =
    Encoder.encodeString.contramap[PointResampleMethod] {
      case NearestNeighbor  => "nearest-neighbor"
      case Bilinear         => "bilinear"
      case CubicConvolution => "cubic-convolution"
      case CubicSpline      => "cubic-spline"
      case Lanczos          => "lanczos"
    }

  implicit val pointResampleMethodDecoder: Decoder[PointResampleMethod] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(str match {
        case "nearest-neighbor"  => NearestNeighbor
        case "bilinear"          => Bilinear
        case "cubic-convolution" => CubicConvolution
        case "cubic-spline"      => CubicSpline
        case "lanczos"           => Lanczos
        case _ =>
          throw new Exception("PointResampleMethod must be a valid string.")
      }).leftMap(_ => "PointResampleMethod")
    }

  implicit val cellSizeEncoder: Encoder[CellSize] =
    Encoder.encodeString.contramap[CellSize] { sz =>
      Json.obj(
        "width" -> sz.width.asJson,
        "height" -> sz.height.asJson
      ).asJson.noSpaces
    }

  implicit val cellSizeDecoder: Decoder[CellSize] =
    Decoder.decodeJsonObject.emap { jso: JsonObject =>
      val map = jso.toMap
      val cellSize = ((map.get("width") |@| map.get("height")) map {
        (w, h) => (new EitherOps(w.as[Double]).toOption |@| new EitherOps(h.as[Double]).toOption) map {
          (width, height) => CellSize(width, height)
        }
      }).flatten

      Either.cond(cellSize.isDefined, cellSize.get, s"Can't decode CellSize: $jso")
    }

  implicit val cellTypeEncoder: Encoder[CellType] =
    Encoder.encodeString.contramap[CellType] { _.toString }
  implicit val cellTypeDecoder: Decoder[CellType] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(CellType.fromName(str)).leftMap(_ => "CellType")
    }
}
