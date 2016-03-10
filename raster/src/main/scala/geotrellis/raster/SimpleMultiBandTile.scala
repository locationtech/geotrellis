package geotrellis.raster

trait SimpleMultiBandTile {
  def bands(bandSequence: Seq[Int]): MultiBandTile

  def bands(bandSequence: Int*)(implicit d: DummyImplicit): MultiBandTile

  def subset(bandSequence: Seq[Int]): MultiBandTile

  def subset(bandSequence: Int*)(implicit d: DummyImplicit): MultiBandTile
}
