/*
 * Copyright 2016 Azavea
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

package geotrellis.raster.io.json

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

import geotrellis.raster._
import geotrellis.vector._

object Implicits extends Implicits

trait Implicits extends HistogramJsonFormats {

  implicit val cellTypeEncoder: Encoder[CellType] =
    Encoder.encodeString.contramap[CellType] { _.toString }
  implicit val cellTypeDecoder: Decoder[CellType] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(CellType.fromName(str)).leftMap(_ => "CellType")
    }

  implicit val cellSizeEncoder: Encoder[CellSize] =
    Encoder.encodeString.contramap[CellSize] { sz =>
      JsonObject.fromMap(
        Map(
          "width" -> sz.width.asJson,
          "height" -> sz.height.asJson
        )
      ).asJson.noSpaces
    }

  implicit val cellSizeDecoder: Decoder[CellSize] =
    Decoder[Json] emap { js =>
      js.as[JsonObject].map { jso =>
        val map = jso.toMap
        (map.get("width"), map.get("height")) match {
          case (Some(width), Some(height)) => {
            (width.as[Double].toOption, height.as[Double].toOption) match {
              case (Some(width), Some(height)) => CellSize(width, height)
              case value => throw new Exception(s"Can't decode CellSize: $value")
            }
          }
          case value => throw new Exception(s"Can't decode CellSize: $value")
        }
      } leftMap (_ => "CellSize")
    }

  implicit val extentEncoder: Encoder[Extent] =
    new Encoder[Extent] {
      def apply(extent: Extent): Json =
        List(extent.xmin, extent.ymin, extent.xmax, extent.ymax).asJson
    }
  implicit val extentDecoder: Decoder[Extent] =
    Decoder[Json] emap { js =>
      js.as[List[Double]].map { case List(xmin, ymin, xmax, ymax) =>
        Extent(xmin, ymin, xmax, ymax)
      }.leftMap(_ => "Extent")
    }

  implicit val rasterExtentEncoder: Encoder[RasterExtent] =
    Encoder.encodeJson.contramap[RasterExtent] { rasterExtent =>
      Json.obj(
        "extent" -> rasterExtent.extent.asJson,
        "cols" -> rasterExtent.cols.asJson,
        "rows" -> rasterExtent.rows.asJson,
        "cellwidth" -> rasterExtent.cellwidth.asJson,
        "cellheight" -> rasterExtent.cellheight.asJson
      )
    }

  implicit val rasterExtentDecoder: Decoder[RasterExtent] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("extent").as[Extent],
      c.downField("cols").as[Int],
      c.downField("rows").as[Int],
      c.downField("cellwidth").as[Double],
      c.downField("cellheight").as[Double]) match {
        case (Right(ext), Right(cols), Right(rows), Right(cw), Right(ch)) => Right(RasterExtent(ext, cw, ch, cols, rows))
        case _ => throw new Exception("RasterExtent expected.")
      }
    }

  implicit val tileLayoutEncoder: Encoder[TileLayout] =
    Encoder.encodeJson.contramap[TileLayout] { tileLayout =>
      Json.obj(
        "layoutCols" -> tileLayout.layoutCols.asJson,
        "layoutRows" -> tileLayout.layoutRows.asJson,
        "tileCols" -> tileLayout.tileCols.asJson,
        "tileRows" -> tileLayout.tileRows.asJson
      )
    }

  implicit val tileLayoutDecoder: Decoder[TileLayout] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("layoutCols").as[Int],
        c.downField("layoutRows").as[Int],
        c.downField("tileCols").as[Int],
        c.downField("tileRows").as[Int]) match {
        case (Right(layoutCols), Right(layoutRows), Right(tileCols), Right(tileRows)) =>
          Right(TileLayout(layoutCols, layoutRows, tileCols, tileRows))
        case _ => throw new Exception("TileLayout expected.")
      }
    }

  implicit val gridBoundsEncoder: Encoder[GridBounds] =
    Encoder.encodeJson.contramap[GridBounds] { gridBounds =>
      Json.obj(
        "colMin" -> gridBounds.colMin.asJson,
        "rowMin" -> gridBounds.rowMin.asJson,
        "colMax" -> gridBounds.colMax.asJson,
        "rowMax" -> gridBounds.rowMax.asJson
      )
    }

  implicit val gridBoundsDecoder: Decoder[GridBounds] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("colMin").as[Int],
        c.downField("rowMin").as[Int],
        c.downField("colMax").as[Int],
        c.downField("rowMax").as[Int]) match {
        case (Right(colMin), Right(rowMin), Right(colMax), Right(rowMax)) =>
          Right(GridBounds(colMin, rowMin, colMax, rowMax))
        case _ => throw new Exception("GridBounds expected.")
      }
    }
}
