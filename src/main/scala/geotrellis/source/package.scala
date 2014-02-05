package geotrellis

package object source {
  type SeqSource[+T] = DataSource[T,Seq[T]]
  type HistogramSource = DataSource[statistics.Histogram,statistics.Histogram]

  implicit def iterableRasterSourceToRasterSourceSeq(iterable:Iterable[RasterSource]):RasterSourceSeq =
    RasterSourceSeq(iterable.toSeq)

  implicit def dataSourceSeqToSeqSource[T](iterable:Iterable[DataSource[_,T]]): SeqSource[T] =
    DataSource.fromSources(iterable.toSeq)

  implicit class DataSourceSeqWrapper[T](dss: Seq[DataSource[_,T]]) {
    def collectSources():SeqSource[T] = DataSource.fromSources(dss)
  }
}
