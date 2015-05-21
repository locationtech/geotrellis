package geotrellis.spark

/**
 * Accumulation of [[RasterRDDFilter]]s that will be asked to filter layer [[KeyBounds]]
 */
class RasterRDDQuery[K: Boundable](
  filters: Tuple2[RasterMetaData, List[KeyBounds[K]]] => Tuple2[RasterMetaData, List[KeyBounds[K]]] =
  { x: Tuple2[RasterMetaData, List[KeyBounds[K]]] => x }) {

  // Allows us to treat Function1 as an instance of a Functor
  import scalaz.Scalaz._

  def filter[T](paramsList: T*)(implicit rasterFilter: RasterRDDFilter[K, T]): RasterRDDQuery[K] = {
    new RasterRDDQuery[K]( {
      filters map { case (metadata, keyBoundsList) =>
        val filteredKeyBounds =
          for {
            keyBound <- keyBoundsList
            param <- paramsList
          } yield {
            rasterFilter(metadata, keyBound, param)
          }
        (metadata, filteredKeyBounds.flatten)
      }
    })
  }

  /**
   * @param metadata RasterMetaData of the layer being queried
   * @param keyBounds Maximum [[KeyBounds]] of the layer
   * @return A sequence of [[KeyBounds]] that cover the queried region
   */
  def apply(metadata: RasterMetaData, keyBounds: KeyBounds[K]): Seq[KeyBounds[K]] = {
    val (_, keyBoundsList) = filters((metadata, List(keyBounds)))
    keyBoundsList
  }
}

/**
 * Wrapper for [[RasterRDDQuery]] that binds it to some function that is able to produce a [[RasterRDD]].
 */
class BoundRasterRDDQuery[K](query: RasterRDDQuery[K], f: RasterRDDQuery[K] => RasterRDD[K]) {
  def filter[T](paramsList: T*)(implicit rasterFilter: RasterRDDFilter[K, T]) =
    new BoundRasterRDDQuery(query.filter(paramsList:_*), f)

  def toRDD: RasterRDD[K] = f(query)
}