package geotrellis.pointcloud.pipeline

import json._
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.syntax._

sealed trait PipelineExpr {
  def ~(other: PipelineExpr): PipelineConstructor =
    PipelineConstructor(this :: other :: Nil)
}

case class Read(
  filename: String,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: Option[ReaderType] = None // usually auto derived by pdal
) extends PipelineExpr

case class FauxRead(
  numPoints: Int,
  mode: String, // constant | random | ramp | uniform | normal
  stdevX: Option[Int] = None, // [default: 1]
  stdevY: Option[Int] = None, // [default: 1]
  stdevZ: Option[Int] = None, // [default: 1]
  meanX: Option[Int] = None, // [default: 0]
  meanY: Option[Int] = None, // [default: 0]
  meanZ: Option[Int] = None, // [default: 0]
  bounds: Option[String] = None, // [default: unit cube]
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = faux
) extends PipelineExpr

object ReadGdal {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(gdal))
}

case class GeoWaveRead(
  zookeeperUrl: String,
  instanceName: String,
  username: String,
  password: String,
  tableNamespace: String,
  featureTypeName: Option[String] = None, // [default: PDAL_Point]
  dataAdapter: Option[String] = None, // [default: FeatureCollectionDataAdapter]
  pointsPerEntry: Option[String] = None, // [default: 5000u]
  bounds: Option[String] = None, // [default: unit cube]
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = geowave
) extends PipelineExpr

case class GreyhoundRead(
  url: String,
  bounds: Option[String] = None, // [default: the entire resource]
  depthBegin: Option[Int] = None, // [default: 0]
  depthEnd: Option[Int] = None, // [default: 0]
  tilePath: Option[String] = None,
  filter: Option[Json] = None,
  threads: Option[Int] = None, // [default: 4]
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = greyhound
) extends PipelineExpr

case class Ilvis2Read(
  filename: String,
  mapping: Option[String] = None,
  metadata: Option[String] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = ilvis2
) extends PipelineExpr

case class LasRead(
  filename: String,
  extraDims: Option[String] = None,
  compression: Option[String] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = las
) extends PipelineExpr

object ReadMrsid {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(mrsid))
}

object ReadNitf {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(nitf))
}

case class OciRead(
  connection: String,
  query: String,
  xmlSchemaDump: Option[String] = None,
  populatePointsourceid: Option[String] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = oci
) extends PipelineExpr

object ReadOptech {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(optech))
}

object ReadPcd {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(pcd))
}

case class PgpointcloudRead(
  connection: String,
  table: String,
  schema: Option[String] = None, // [default: public]
  column: Option[String] = None, // [default: pa]
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = pgpointcloud
) extends PipelineExpr

object ReadPly {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(ply))
}

object ReadPts {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(pts))
}

case class QfitRead(
  filename: String,
  flipCoordinates: Option[Boolean] = None,
  scaleZ: Option[Double] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = qfit
) extends PipelineExpr

case class RxpRead(
  filename: String,
  rdtp: Option[Boolean] = None,
  syncToPps: Option[Boolean] = None,
  minimal: Option[Boolean] = None,
  reflectanceAsIntensity: Option[Boolean] = None,
  minReflectance: Option[Double] = None,
  maxReflectance: Option[Double] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = rxp
) extends PipelineExpr

object ReadSbet {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(sbet))
}

case class SqliteRead(
  connection: String,
  query: String,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = sqlite
) extends PipelineExpr

object ReadTxt {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(txt))
}

case class TindexRead(
  filename: String,
  layerName: Option[String] = None,
  srsColumn: Option[String] = None,
  tindexName: Option[String] = None,
  sql: Option[String] = None,
  wkt: Option[String] = None,
  boundary: Option[String] = None,
  tSrs: Option[String] = None,
  filterSrs: Option[String] = None,
  where: Option[String] = None,
  dialect: Option[String] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = tindex
) extends PipelineExpr

// in future we can implement all writers
case class Write(
  filename: String,
  spatialreference: Option[String] = None,
  `type`: Option[String] = None // usually auto derived by pdal
) extends PipelineExpr

case class ApproximateCoplanarFilter(
  knn: Int, // [default: 8]
  thresh1: Int, // [default: 25]
  thresh2: Int, // [default: 6]
  `type`: FilterType = approximatecoplanar
) extends PipelineExpr

case class AttributeFilter(
  dimension: Option[String] = None, // [default: none]
  value: Option[Double] = None, // [default: none]
  datasource: Option[String] = None, // [default: none]
  column: Option[String] = None, // [default: none]
  query: Option[String] = None, // [default: first column]
  layer: Option[String] = None, // [default: first layer]
  `type`: FilterType = attribute
) extends PipelineExpr

case class ChipperFilter(
  capacity: Option[Int] = None, // [default: 5000]
  `type`: FilterType = chipper
) extends PipelineExpr

case class ColorinterpFilter(
  ramp: Option[String] = None, // [default: pestel_shades]
  dimension: Option[String] = None, // [default: Z]
  minimum: Option[String] = None,
  maximum: Option[String] = None,
  invert: Option[Boolean] = None, // [default: false]
  k: Option[Double] = None,
  mad: Option[Boolean] = None,
  madMultiplier: Option[Double] = None,
  `type`: FilterType = colorinterp
) extends PipelineExpr

case class ColorizationFilter(
  raster: String,
  dimensions: Option[String] = None,
  `type`: FilterType = colorization
) extends PipelineExpr

case class ComputerangeFilter(
  `type`: FilterType = computerange
) extends PipelineExpr

case class CropFilter(
  bounds: Option[String] = None,
  polygon: Option[String] = None,
  outside: Option[String] = None,
  point: Option[String] = None,
  radius: Option[String] = None,
  `type`: FilterType = crop
) extends PipelineExpr

case class DecimationFilter(
  step: Option[Int] = None,
  offset: Option[Int] = None,
  limit: Option[Int] = None,
  `type`: FilterType = decimation
) extends PipelineExpr

case class DividerFilter(
   mode: Option[String] = None,
   count: Option[Int] = None,
   capacity: Option[Int] = None,
  `type`: FilterType = divider
) extends PipelineExpr

case class EigenValuesFilter(
  knn: Option[Int] = None,
  `type`: FilterType = eigenvalues
) extends PipelineExpr

case class EstimateRankFilter(
  knn: Option[Int] = None,
  thresh: Option[Double] = None,
  `type`: FilterType = estimaterank
) extends PipelineExpr

case class FerryFilter(
  dimensions: String,
  `type`: FilterType = ferry
) extends PipelineExpr

case class GreedyProjectionFilter(
  `type`: FilterType = greedyprojection
) extends PipelineExpr

case class GridProjectionFilter(
  `type`: FilterType = gridprojection
) extends PipelineExpr

case class HagFilter(
  `type`: FilterType = hag
) extends PipelineExpr

case class HexbinFilter(
  edgeSize: Option[Int] = None,
  sampleSize: Option[Int] = None,
  threshold: Option[Int] = None,
  precision: Option[Int] = None,
  `type`: FilterType = hexbin
) extends PipelineExpr

case class IqrFilter(
  dimension: String,
  k: Option[Double] = None,
  `type`: FilterType = iqr
) extends PipelineExpr

case class KDistanceFilter(
  k: Option[Int] = None,
  `type`: FilterType = kdistance
) extends PipelineExpr

case class LofFilter(
  minpts: Option[Int] = None,
  `type`: FilterType = lof
) extends PipelineExpr

case class MadFilter(
  dimension: String,
  k: Option[Double] = None,
  `type`: FilterType = mad
) extends PipelineExpr

case class MergeFilter(
  inputs: List[String],
  tag: Option[String] = None,
  `type`: FilterType = merge
) extends PipelineExpr

case class MongusFilter(
  cell: Option[Double] = None,
  classify: Option[Boolean] = None,
  extract: Option[Boolean] = None,
  k: Option[Double] = None,
  l: Option[Int] = None,
  `type`: FilterType = mongus
) extends PipelineExpr

case class MortnOrderFilter(
  `type`: FilterType = mortonorder
) extends PipelineExpr

case class MovingLeastSquaresFilter(
  `type`: FilterType = movingleastsquares
) extends PipelineExpr

case class NormalFilter(
  knn: Option[Int] = None,
  `type`: FilterType = normal
) extends PipelineExpr

case class OutlierFilter(
  method: Option[String] = None,
  minK: Option[Int] = None,
  radius: Option[Double] = None,
  meanK: Option[Int] = None,
  multiplier: Option[Double] = None,
  classify: Option[Boolean] = None,
  extract: Option[Boolean] = None,
  `type`: FilterType = outlier
) extends PipelineExpr

case class PclBlockFilter(
  filename: String,
  methods: Option[List[String]] = None,
  `type`: FilterType = pclblock
) extends PipelineExpr

case class PmfFilter(
  maxWindowSize: Option[Int] = None,
  slope: Option[Double] = None,
  maxDistance: Option[Double] = None,
  initialDistance: Option[Double] = None,
  cellSize: Option[Int] = None,
  classify: Option[Boolean] = None,
  extract: Option[Boolean] = None,
  approximate: Option[Boolean] = None,
  `type`: FilterType = pmf
) extends PipelineExpr

case class PoissonFilter(
  depth: Option[Int] = None,
  pointWeight: Option[Double] = None,
  `type`: FilterType = poisson
) extends PipelineExpr

case class PredicateFilter(
  script: String,
  module: String,
  function: String,
  `type`: FilterType = predicate
) extends PipelineExpr

case class ProgrammableFilter(
  script: String,
  module: String,
  function: String,
  source: String,
  addDimenstion: Option[String] = None,
  `type`: FilterType = programmable
) extends PipelineExpr

case class RadialDensityFilter(
  radius: Option[Double] = None,
  `type`: FilterType = radialdensity
) extends PipelineExpr

case class RandomizeFilter(
  `type`: FilterType = randomize
) extends PipelineExpr

case class RangeFilter(
  limits: Option[String] = None,
  `type`: FilterType = range
) extends PipelineExpr

case class ReprojectionFilter(
  outSrs: String,
  inSrs: Option[String] = None,
  tag: Option[String] = None,
  `type`: FilterType = reprojection
) extends PipelineExpr

case class SampleFilter(
  radius: Option[Double] = None,
  `type`: FilterType = sample
) extends PipelineExpr

case class SmrfFilter(
  cell: Option[Double] = None,
  classify: Option[Boolean] = None,
  cut: Option[Double] = None,
  extract: Option[Boolean] = None,
  slope: Option[Double] = None,
  threshold: Option[Double] = None,
  window: Option[Double] = None,
  `type`: FilterType = smrf
) extends PipelineExpr

case class SortFilter(
  dimension: String,
  `type`: FilterType = sort
) extends PipelineExpr

case class SplitterFilter(
  length: Option[Int] = None,
  originX: Option[Double] = None,
  originY: Option[Double] = None,
  `type`: FilterType = splitter
) extends PipelineExpr

case class StatsFilter(
  dimenstions: Option[String] = None,
  enumerate: Option[String] = None,
  count: Option[Int] = None,
  `type`: FilterType = stats
) extends PipelineExpr

case class TransformationFilter(
  matrix: String,
  `type`: FilterType = transformation
) extends PipelineExpr

case class VoxelGridFilter(
  leafX: Option[Double] = None,
  leafY: Option[Double] = None,
  leafZ: Option[Double] = None,
  `type`: FilterType = voxelgrid
) extends PipelineExpr

case class PipelineConstructor(list: List[PipelineExpr]) {
  def ~(e: PipelineExpr): PipelineConstructor = PipelineConstructor(list :+ e)
  def map[B](f: PipelineExpr => B): List[B] = list.map(f)
  def mapExpr(f: PipelineExpr => PipelineExpr): PipelineConstructor = PipelineConstructor(list.map(f))
  def tail: List[PipelineExpr] = list.tail
  def head: PipelineExpr = list.head
  def json: Json =
    Json.obj(
      "pipeline" -> list
        .map(
          _.asJsonObject
            .remove("class_type") // remove type
            .filter { case (key, value) => !value.isNull } // cleanup options
        ).asJson
    )
}
