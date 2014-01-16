package geotrellis

package object source {
  type SeqSource[+T] = DataSource[T,Seq[T]]
  type HistogramSource = DataSource[statistics.Histogram,statistics.Histogram]

  implicit def seqRasterSourceToRasterSourceSeq(seq:Seq[RasterSource]):RasterSourceSeq =
    RasterSourceSeq(seq)
}
