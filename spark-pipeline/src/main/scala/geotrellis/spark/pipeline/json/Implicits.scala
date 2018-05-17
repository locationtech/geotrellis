/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.pipeline.json

import geotrellis.spark.pipeline.json.read._
import geotrellis.spark.pipeline.json.write._
import geotrellis.spark.pipeline.json.reindex._
import geotrellis.spark.pipeline.json.update._
import geotrellis.spark.pipeline.json.transform._
import geotrellis.proj4.CRS
import geotrellis.spark.pipeline._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util.LazyLogging

import _root_.io.circe.generic.extras.Configuration
import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.generic.extras.semiauto._
import cats.syntax._
import cats.implicits._

import java.net.URI

import scala.util.Try

object Implicits extends Implicits

trait Implicits extends LazyLogging {
  implicit val config: Configuration = Configuration.default.withDefaults.withSnakeCaseMemberNames
  val pipelineJsonPrinter: Printer = Printer.spaces2.copy(dropNullValues = true)

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
      ((json.hcursor.downField("tileCols").as[Int], json.hcursor.downField("tileRows").as[Int]) mapN {
        (tileCols, tileRows) => FloatingLayoutScheme(tileCols, tileRows)
      } match {
        case right @ Right(_) => right
        case Left(_) =>
          (json.hcursor.downField("crs").as[CRS], json.hcursor.downField("tileSize").as[Int], json.hcursor.downField("resolutionThreshold").as[Double]) mapN {
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
      val cellSize = ((map.get("width"), map.get("height")) mapN {
        (w, h) => (new EitherOps(w.as[Double]).toOption, new EitherOps(h.as[Double]).toOption) mapN {
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

  implicit val pipelineExprTypeEncode: Encoder[PipelineExprType] =
    Encoder.encodeString.contramap[PipelineExprType] { _.name }
  implicit val pipelineExprTypeDecode: Decoder[PipelineExprType] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(PipelineExprType.fromName(str)).leftMap(_ => "PipelineExprType")
    }

  // TODO: Implement user defined PipelineExpr Encoders and Decoders support
  implicit val pipelineExprEncode: Encoder[PipelineExpr] =
    Encoder.instance { expr: PipelineExpr =>
      expr match {
        case e: JsonWrite => e.asJson
        case e: JsonRead => e.asJson
        case e: JsonReindex => e.asJson
        case e: JsonUpdate => e.asJson
        case e: Reproject => e.asJson
        case e: TileToLayout => e.asJson
        case e: RetileToLayout => e.asJson
        case e: Pyramid => e.asJson
        case e => throw new Exception(s"Unsupported AST node $e")
      }
    }

  // TODO: Probably json tags support is required, this decoder logic may change in a near future
  implicit val pipelineExprDecode: Decoder[PipelineExpr] =
    Decoder.instance { hcursor =>
      val ftype = hcursor.downField("type")
      ftype.as[PipelineExprType] match {
        case Right(exprType) =>
          exprType match {
            case _: ReadTypes.ReadType   => hcursor.as[JsonRead]
            case _: WriteTypes.WriteType => hcursor.as[JsonWrite]
            case _: TransformTypes.TransformType =>
              exprType match {
                case _: TransformTypes.PerTileReprojectType  => hcursor.as[Reproject]
                case _: TransformTypes.BufferedReprojectType => hcursor.as[Reproject]
                case _: TransformTypes.TileToLayoutType      => hcursor.as[TileToLayout]
                case _: TransformTypes.RetileToLayoutType    => hcursor.as[RetileToLayout]
                case _: TransformTypes.PyramidType           => hcursor.as[Pyramid]
              }
          }
        case Left(e) =>
          throw new UnsupportedOperationException(s"Type $ftype is not supported by a default PipelineExpr decoder").initCause(e)
      }
    }
}
