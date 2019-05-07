package geotrellis.layers.cog

import geotrellis.layers.Reader
import geotrellis.raster.Tile


trait COGReader[K, V] extends Reader[K, V] {
  def readSubsetBands(key: K, bands: Int*)(implicit d: DummyImplicit): Array[Option[Tile]] =
    readSubsetBands(key, bands.toSeq)

  def readSubsetBands(key: K, bands: Seq[Int]): Array[Option[Tile]]
}
