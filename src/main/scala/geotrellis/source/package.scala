package geotrellis

package object source {
  type DS[T,V] = DataSource[T,V]
  type RasterDS = RasterDataSource
  type ValueDS[T] = ValueDataSource[T]
}
