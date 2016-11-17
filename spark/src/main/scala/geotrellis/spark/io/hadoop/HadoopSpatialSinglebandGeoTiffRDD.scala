package geotrellis.spark.io.hadoop

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.split._
import geotrellis.spark.io.hadoop._
import geotrellis.vector.ProjectedExtent

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.spark.api.java._
//SparkContext
import org.apache.spark.rdd.RDD

class HadoopSpatialSinglebandGeoTiffRDD(path: String, jsc: JavaSparkContext) {
	implicit val context = jsc.sc

	val hadoopPath: Path = new Path(path)
	val rdd: RDD[(ProjectedExtent, Tile)] = HadoopGeoTiffRDD.spatial(hadoopPath)

	def getKey: ProjectedExtent = rdd.first._1
	def getValue: Tile = rdd.first._2

	def split(totalCols: Int, totalRows: Int): RDD[(ProjectedExtent, Tile)] =
		rdd.split(totalCols, totalRows)
}
