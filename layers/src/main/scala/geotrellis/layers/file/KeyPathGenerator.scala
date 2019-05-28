package geotrellis.layers.file

import java.io.File

import geotrellis.layers.index.{Index, KeyIndex}

object KeyPathGenerator {
  def apply[K](catalogPath: String, layerPath: String, keyIndex: KeyIndex[K], maxWidth: Int): K => String = {
    val f = new File(catalogPath, layerPath)
    (key: K) => new File(f, Index.encode(keyIndex.toIndex(key), maxWidth)).getAbsolutePath
  }

  def apply(catalogPath: String, layerPath: String, maxWidth: Int): BigInt => String = {
    val f = new File(catalogPath, layerPath)
    (index: BigInt) => new File(f, Index.encode(index, maxWidth)).getAbsolutePath
  }
}
