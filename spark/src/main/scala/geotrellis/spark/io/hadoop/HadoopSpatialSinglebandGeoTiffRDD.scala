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
import org.apache.spark.rdd.RDD

class HadoopSpatialSinglebandGeoTiffRDD(path: String, jsc: JavaSparkContext) {
	implicit val context = jsc.sc

	val hadoopPath: Path = new Path(path)
	val rdd: RDD[(ProjectedExtent, Tile)] = HadoopGeoTiffRDD.spatial(hadoopPath)

	def getKey: ProjectedExtent = rdd.first._1
	def getValue: Tile = rdd.first._2

	def split(totalCols: java.lang.Integer, totalRows: java.lang.Integer): RDD[(ProjectedExtent, Tile)] =
		rdd.split(totalCols.intValue, totalRows.intValue)

	def fromEpsgCode(code: Int): CRS =
		CRS.fromEpsgCode(code)

	def reproject(target: RDD[(ProjectedExtent, Tile)], dest: CRS): RDD[(ProjectedExtent, Tile)] =
		target.map(x => (ProjectedExtent(x._1.reproject(dest), dest), x._2))
}
