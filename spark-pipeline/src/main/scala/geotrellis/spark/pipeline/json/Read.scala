package geotrellis.spark.pipeline.json

import geotrellis.spark.pipeline._
import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.io.hadoop.HadoopGeoTiffRDD
import geotrellis.vector.ProjectedExtent

import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import java.net.URI

trait Read extends PipelineExpr {
  val profile: String
  val uri: String
  val crs: Option[String]
  val tag: Option[String]
  val maxTileSize: Option[Int]
  val partitions: Option[Int]
  val clip: Boolean

  def getURI = new URI(uri)
  def getTag = tag.getOrElse("default")
}

case class ReadS3(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
) extends Read

case class SpatialReadHadoop(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
)(implicit sc: SparkContext) extends Read {
  def eval: RDD[(ProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.spatial(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions
      )
    )
  }
}

case class SpatialMultibandReadHadoop(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
)(implicit sc: SparkContext) extends Read {
  def eval: RDD[(ProjectedExtent, MultibandTile)] = {
    HadoopGeoTiffRDD.spatialMultiband(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions
      )
    )
  }
}

case class TemporalReadHadoop(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
)(implicit sc: SparkContext) extends Read {
  def eval: RDD[(TemporalProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.temporal(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions
      )
    )
  }
}

case class TemporalMultibandReadHadoop(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
)(implicit sc: SparkContext) extends Read {
  def eval: RDD[(TemporalProjectedExtent, MultibandTile)] = {
    HadoopGeoTiffRDD.temporalMultiband(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions
      )
    )
  }
}

