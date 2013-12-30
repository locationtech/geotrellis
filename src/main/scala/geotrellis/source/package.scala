package geotrellis

package object source {
  implicit def seqRasterSourceToRasterSourceSeq(seq:Seq[RasterSource]):RasterSourceSeq =
    RasterSourceSeq(seq)
}
