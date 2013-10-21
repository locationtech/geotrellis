package geotrellis

package object source {
  type DS[T,V] = DataSource[T,V]
  type RasterDS = RasterDataSource
  type ValueDataSource[T] = ValueDS[T]
}
