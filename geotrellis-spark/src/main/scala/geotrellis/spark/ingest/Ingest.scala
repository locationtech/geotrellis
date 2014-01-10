package geotrellis.spark.ingest
import org.apache.hadoop.conf.Configuration

import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import scala.collection.mutable.ListBuffer

object Ingest {
	val Default_Projection = "EPSG:4326"
}