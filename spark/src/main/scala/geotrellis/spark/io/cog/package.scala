package geotrellis.spark.io

import org.apache.spark.util.AccumulatorV2
import java.util

package object cog extends Implicits {
  type MetadataAccumulator[M] = AccumulatorV2[(Int, M), util.Map[Int, M]]
}
