package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.vector._
import geotrellis.spark.io.hadoop.formats._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.conf.Configuration

trait HadoopSparkContextMethods {
  val sc: SparkContext
  val defaultTiffExtensions: Seq[String] = Seq(".tif", ".TIF", ".tiff", ".TIFF")

  def hadoopGeoTiffRDD(path: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), defaultTiffExtensions)

  def hadoopGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: String, tiffExtensions: Seq[String] ): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopGeoTiffRDD(path: Path): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, defaultTiffExtensions)

  def hadoopGeoTiffRDD(path: Path, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: Path, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, Tile)] =
    sc.newAPIHadoopRDD(
      sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions),
      classOf[GeotiffInputFormat],
      classOf[ProjectedExtent],
      classOf[Tile]
    )

  def hadoopTemporalGeoTiffRDD(path: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), defaultTiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: String, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopTemporalGeoTiffRDD(path: String, tiffExtensions: Seq[String] ): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: Path): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(path, defaultTiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: Path, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopTemporalGeoTiffRDD(path: Path, tiffExtensions: Seq[String]): RDD[(TemporalProjectedExtent, Tile)] =
    sc.newAPIHadoopRDD(
      sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions),
      classOf[TemporalGeoTiffInputFormat],
      classOf[TemporalProjectedExtent],
      classOf[Tile]
    )

  def hadoopMultiBandGeoTiffRDD(path: String): RDD[(ProjectedExtent, MultiBandTile)] =
    hadoopMultiBandGeoTiffRDD(new Path(path), defaultTiffExtensions)

  def hadoopMultiBandGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, MultiBandTile)] =
    hadoopMultiBandGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopMultiBandGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, MultiBandTile)] =
    hadoopMultiBandGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopMultiBandGeoTiffRDD(path: Path, tiffExtensions: Seq[String] = defaultTiffExtensions): RDD[(ProjectedExtent, MultiBandTile)] =
    sc.newAPIHadoopRDD(
      sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions),
      classOf[MultiBandGeoTiffInputFormat],
      classOf[ProjectedExtent],
      classOf[MultiBandTile]
    )

  def newJob: Job =
    Job.getInstance(sc.hadoopConfiguration)

  def newJob(name: String) =
    Job.getInstance(sc.hadoopConfiguration, name)
}
