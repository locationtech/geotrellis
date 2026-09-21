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

package geotrellis.spark.pipeline.json.transform

import geotrellis.raster.resample.{NearestNeighbor, PointResampleMethod}
import geotrellis.raster.CellType
import geotrellis.spark.pipeline.json.*
import geotrellis.layer.{LayoutDefinition, LayoutScheme}
import io.circe.{Decoder, Encoder}
import geotrellis.spark.pipeline.json.CodecUtils.orDefault

trait Transform extends PipelineExpr

// TODO: implement node for these PipelineExpressions
// Not all functions are yet supported
/** Rename Inputs into groups */
case class Group(
  tags: List[String],
  tag: String,
  `type`: PipelineExprType
) extends Transform

/** Merge inputs into a single Multiband RDD */
case class Merge(
  tags: List[String],
  tag: String,
  `type`: PipelineExprType
) extends Transform

case class Map(
  func: String, // function name
  tag: Option[String] = None,
  `type`: PipelineExprType
) extends Transform

case class Reproject(
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None,
  `type`: PipelineExprType
) extends Transform

case class TileToLayout(
  resampleMethod: PointResampleMethod = NearestNeighbor,
  tileSize: Option[Int] = None,
  cellType: Option[CellType] = None,
  `type`: PipelineExprType
) extends Transform

case class RetileToLayout(
  layoutDefinition: LayoutDefinition,
  resampleMethod: PointResampleMethod = NearestNeighbor,
  `type`: PipelineExprType
) extends Transform

case class Pyramid(
  startZoom: Option[Int] = None,
  endZoom: Option[Int] = Some(0),
  resampleMethod: PointResampleMethod = NearestNeighbor,
  `type`: PipelineExprType
) extends Transform

object Group {
  implicit val groupEncoder: Encoder[Group] =
    Encoder.forProduct3("tags", "tag", "type") { g => (g.tags, g.tag, g.`type`) }
  implicit val groupDecoder: Decoder[Group] = Decoder.instance { c =>
    for {
      tags <- c.get[List[String]]("tags")
      tag  <- c.get[String]("tag")
      tpe  <- c.get[PipelineExprType]("type")
    } yield Group(tags, tag, tpe)
  }
}

object Merge {
  implicit val mergeEncoder: Encoder[Merge] =
    Encoder.forProduct3("tags", "tag", "type") { m => (m.tags, m.tag, m.`type`) }
  implicit val mergeDecoder: Decoder[Merge] = Decoder.instance { c =>
    for {
      tags <- c.get[List[String]]("tags")
      tag  <- c.get[String]("tag")
      tpe  <- c.get[PipelineExprType]("type")
    } yield Merge(tags, tag, tpe)
  }
}

object Map {
  implicit val mapEncoder: Encoder[Map] =
    Encoder.forProduct3("func", "tag", "type") { m => (m.func, m.tag, m.`type`) }
  implicit val mapDecoder: Decoder[Map] = Decoder.instance { c =>
    for {
      func <- c.get[String]("func")
      tag  <- orDefault[Option[String]](c, "tag", None)
      tpe  <- c.get[PipelineExprType]("type")
    } yield Map(func, tag, tpe)
  }
}

object Reproject {
  implicit val reprojectEncoder: Encoder[Reproject] =
    Encoder.forProduct5("crs", "scheme", "resample_method", "max_zoom", "type") { r =>
      (r.crs, r.scheme, r.resampleMethod, r.maxZoom, r.`type`)
    }
  implicit val reprojectDecoder: Decoder[Reproject] = Decoder.instance { c =>
    for {
      crs <- c.get[String]("crs")
      sch <- c.get[Either[LayoutScheme, LayoutDefinition]]("scheme")
      rm  <- orDefault[PointResampleMethod](c, "resample_method", NearestNeighbor)
      mz  <- orDefault[Option[Int]](c, "max_zoom", None)
      tpe <- c.get[PipelineExprType]("type")
    } yield Reproject(crs, sch, rm, mz, tpe)
  }
}

object TileToLayout {
  implicit val tileToLayoutEncoder: Encoder[TileToLayout] =
    Encoder.forProduct4("resample_method", "tile_size", "cell_type", "type") { t =>
      (t.resampleMethod, t.tileSize, t.cellType, t.`type`)
    }
  implicit val tileToLayoutDecoder: Decoder[TileToLayout] = Decoder.instance { c =>
    for {
      rm  <- orDefault[PointResampleMethod](c, "resample_method", NearestNeighbor)
      ts  <- orDefault[Option[Int]](c, "tile_size", None)
      ct  <- orDefault[Option[CellType]](c, "cell_type", None)
      tpe <- c.get[PipelineExprType]("type")
    } yield TileToLayout(rm, ts, ct, tpe)
  }
}

object RetileToLayout {
  implicit val retileToLayoutEncoder: Encoder[RetileToLayout] =
    Encoder.forProduct3("layout_definition", "resample_method", "type") { r =>
      (r.layoutDefinition, r.resampleMethod, r.`type`)
    }
  implicit val retileToLayoutDecoder: Decoder[RetileToLayout] = Decoder.instance { c =>
    for {
      ld  <- c.get[LayoutDefinition]("layout_definition")
      rm  <- orDefault[PointResampleMethod](c, "resample_method", NearestNeighbor)
      tpe <- c.get[PipelineExprType]("type")
    } yield RetileToLayout(ld, rm, tpe)
  }
}

object Pyramid {
  implicit val pyramidEncoder: Encoder[Pyramid] =
    Encoder.forProduct4("start_zoom", "end_zoom", "resample_method", "type") { p =>
      (p.startZoom, p.endZoom, p.resampleMethod, p.`type`)
    }
  implicit val pyramidDecoder: Decoder[Pyramid] = Decoder.instance { c =>
    for {
      sz  <- orDefault[Option[Int]](c, "start_zoom", None)
      ez  <- orDefault[Option[Int]](c, "end_zoom", Some(0))
      rm  <- orDefault[PointResampleMethod](c, "resample_method", NearestNeighbor)
      tpe <- c.get[PipelineExprType]("type")
    } yield Pyramid(sz, ez, rm, tpe)
  }
}
