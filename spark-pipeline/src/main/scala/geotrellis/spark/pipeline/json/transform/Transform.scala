package geotrellis.spark.pipeline.json.transform

import geotrellis.raster.resample.{NearestNeighbor, PointResampleMethod}
import geotrellis.raster.CellType
import geotrellis.spark.pipeline.json._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import io.circe.generic.extras.ConfiguredJsonCodec

trait Transform extends PipelineExpr

/** Rename Inputs into groups */
@ConfiguredJsonCodec
case class Group(
  tags: List[String],
  tag: String,
  `type`: String = "transform.group"
) extends Transform

/** Merge inputs into a single Multiband RDD */
@ConfiguredJsonCodec
case class Merge(
  tags: List[String],
  tag: String,
  `type`: String = "transform.merge"
) extends Transform

@ConfiguredJsonCodec
case class Map(
  func: String, // function name
  tag: Option[String] = None,
  `type`: String = "transform.map"
) extends Transform

@ConfiguredJsonCodec
case class PerTileReproject(
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None,
  `type`: String = "transform.reproject.per-tile"
) extends Transform

@ConfiguredJsonCodec
case class BufferedReproject(
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None,
  `type`: String = "transform.reproject.buffered"
) extends Transform

@ConfiguredJsonCodec
case class TileToLayout(
  resampleMethod: PointResampleMethod = NearestNeighbor,
  tileSize: Option[Int] = None,
  cellType: Option[CellType] = None,
  `type`: String = "transform.tile-to-layout"
) extends Transform

@ConfiguredJsonCodec
case class RetileToLayout(
  layoutDefinition: LayoutDefinition,
  resampleMethod: PointResampleMethod = NearestNeighbor,
  `type`: String = "transform.tile-to-layout"
) extends Transform

@ConfiguredJsonCodec
case class Pyramid(
  startZoom: Option[Int] = None,
  endZoom: Option[Int] = Some(0),
  resampleMethod: PointResampleMethod = NearestNeighbor,
  `type`: String = "transform.pyramid"
) extends Transform