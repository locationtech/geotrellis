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
import geotrellis.spark.pipeline.json._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import io.circe.generic.extras.ConfiguredJsonCodec

trait Transform extends PipelineExpr

// TODO: implement node for these PipelineExpressions
// Not all functions are yet supported
/** Rename Inputs into groups */
@ConfiguredJsonCodec
case class Group(
  tags: List[String],
  tag: String,
  `type`: PipelineExprType
) extends Transform

/** Merge inputs into a single Multiband RDD */
@ConfiguredJsonCodec
case class Merge(
  tags: List[String],
  tag: String,
  `type`: PipelineExprType
) extends Transform

@ConfiguredJsonCodec
case class Map(
  func: String, // function name
  tag: Option[String] = None,
  `type`: PipelineExprType
) extends Transform

@ConfiguredJsonCodec
case class Reproject(
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None,
  `type`: PipelineExprType
) extends Transform

@ConfiguredJsonCodec
case class TileToLayout(
  resampleMethod: PointResampleMethod = NearestNeighbor,
  tileSize: Option[Int] = None,
  cellType: Option[CellType] = None,
  `type`: PipelineExprType
) extends Transform

@ConfiguredJsonCodec
case class RetileToLayout(
  layoutDefinition: LayoutDefinition,
  resampleMethod: PointResampleMethod = NearestNeighbor,
  `type`: PipelineExprType
) extends Transform

@ConfiguredJsonCodec
case class Pyramid(
  startZoom: Option[Int] = None,
  endZoom: Option[Int] = Some(0),
  resampleMethod: PointResampleMethod = NearestNeighbor,
  `type`: PipelineExprType
) extends Transform