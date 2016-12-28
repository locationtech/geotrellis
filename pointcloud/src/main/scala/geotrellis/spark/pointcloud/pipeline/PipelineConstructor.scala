package geotrellis.spark.pointcloud.pipeline

import json._
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.syntax._

sealed trait PipelineExpr

case class Read(
  filename: String,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: Option[ReaderType] = None // usually auto derived by pdal
) extends PipelineExpr

case class ReadFaux(
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

case class ReadGeoWave(
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

case class ReadGreyhound(
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

case class ReadIlvis2(
  filename: String,
  mapping: Option[String] = None,
  metadata: Option[String] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = ilvis2
) extends PipelineExpr

case class ReadLas(
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

case class ReadOci(
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

case class ReadPgpointcloud(
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

case class ReadQfit(
  filename: String,
  flipCoordinates: Option[Boolean] = None,
  scaleZ: Option[Double] = None,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: ReaderType = qfit
) extends PipelineExpr

case class ReadRxp(
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

case class ReadSqlite(
  connection: String,
  query: String,
  spatialreference: Option[String] = None,
  tag: Option[String] = None,
  `type`: String = "readers.sqlite"
) extends PipelineExpr

object ReadTxt {
  def apply(filename: String, spatialreference: Option[String] = None, tag: Option[String] = None): Read =
    Read(filename, spatialreference, tag, Some(txt))
}

case class ReadTindex(
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

case class Reproject(
  outSrs: String,
  inSrs: Option[String] = None,
  tag: Option[String] = None,
  `type`: String = "filters.reprojection"
) extends PipelineExpr

case class Merge(
  inputs: List[String],
  tag: Option[String] = None,
  `type`: String = "filters.merge"
) extends PipelineExpr

case class Write(
  filename: String,
  spatialreference: Option[String] = None,
  `type`: Option[String] = None // usually auto derived by pdal
) extends PipelineExpr

case class ApproximateCoplanar(
  knn: Int, // [default: 8]
  thresh1: Int, // [default: 25]
  thresh2: Int, // [default: 6]
  `type`: String = "filters.approximatecoplanar"
) extends PipelineExpr

case class Attribute(
  dimension: Option[String] = None, // [default: none]
  value: Option[Double] = None, // [default: none]
  datasource: Option[String] = None, // [default: none]
  column: Option[String] = None, // [default: none]
  query: Option[String] = None, // [default: first column]
  layer: Option[String] = None, // [default: first layer]
  `type`: String = "filters.attribtue"
) extends PipelineExpr

case class PipelineConstructor(list: List[PipelineExpr]) {
  def ~(e: PipelineExpr): PipelineConstructor = PipelineConstructor(list :+ e)
  def map[B](f: PipelineExpr => B): List[B] = list.map(f)
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
