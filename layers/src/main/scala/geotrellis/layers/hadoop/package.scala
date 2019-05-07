package geotrellis.layers

import org.apache.hadoop.fs.Path

package object hadoop extends Implicits {
  implicit def stringToPath(path: String): Path = new Path(path)
}
