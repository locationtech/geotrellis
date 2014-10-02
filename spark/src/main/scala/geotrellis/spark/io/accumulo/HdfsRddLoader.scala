package geotrellis.spark.io.accumulo

import org.apache.hadoop.fs.Path

trait HdfsRddLoader[K] extends RddLoader[K, HdfsRddSource]{
  def load[K](path: Path, filters: AccumuloFilter*)
}
