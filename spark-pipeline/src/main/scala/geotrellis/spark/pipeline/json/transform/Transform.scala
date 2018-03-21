package geotrellis.spark.pipeline.json.transform

import geotrellis.raster.resample.{NearestNeighbor, PointResampleMethod}
import geotrellis.raster.CellType
import geotrellis.spark.pipeline.json._
import geotrellis.tiling.{LayoutDefinition, LayoutScheme}
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
