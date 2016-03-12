package geotrellis.raster

trait SimpleMultiBandTile {
  def bands(bandSequence: Seq[Int]): MultiBandTile

  def bands(bandSequence: Int*)(implicit d: DummyImplicit): MultiBandTile
}
