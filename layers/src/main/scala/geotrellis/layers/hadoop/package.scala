package geotrellis.layers

import org.apache.hadoop.fs.Path

package object hadoop extends Implicits {
  final val SCHEMES: Array[String] = Array("hdfs", "hdfs+file", "s3n", "s3a", "wasb", "wasbs")

  implicit def stringToPath(path: String): Path = new Path(path)
}
