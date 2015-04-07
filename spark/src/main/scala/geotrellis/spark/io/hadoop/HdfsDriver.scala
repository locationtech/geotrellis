package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.formats._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

import scala.util.Try

// trait HdfsDriver[K] extends Driver[K]{
//   type Params = Path

//   def load[K: HadoopWritable: Ordering](sc: SparkContext)(path: Path, metaData: LayerMetaData): Try[RasterRDD[K]] = 
//     load(sc)(path, metaData, FilterSet[K]())

//   def load[K: HadoopWritable: Ordering](sc: SparkContext)(path: Path, metaData: LayerMetaData, filters: FilterSet[K]): Try[RasterRDD[K]] = ???
// }
