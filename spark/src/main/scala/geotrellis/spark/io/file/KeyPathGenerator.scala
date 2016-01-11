package geotrellis.spark.io.file

import geotrellis.spark.io.index._

import java.io.File

object KeyPathGenerator {
  def apply[K](catalogPath: String, layerPath: String, keyIndex: KeyIndex[K], maxWidth: Int): K => String = {
    val f = new File(catalogPath, layerPath)
    (key: K) => new File(f, Index.encode(keyIndex.toIndex(key), maxWidth)).getAbsolutePath
  }

  def apply(catalogPath: String, layerPath: String, maxWidth: Int): Long => String = {
    val f = new File(catalogPath, layerPath)
    (index: Long) => new File(f, Index.encode(index, maxWidth)).getAbsolutePath
  }
}
