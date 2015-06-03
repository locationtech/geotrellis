
package geotrellis.spark

/**
 * Accumulation of [[RasterRDDFilter]]s that will be asked to filter layer [[KeyBounds]]
 */
class RasterRDDQuery[K: Boundable](
  filterChain: Tuple2[RasterMetaData, List[KeyBounds[K]]] => Tuple2[RasterMetaData, List[KeyBounds[K]]] =
  { x: Tuple2[RasterMetaData, List[KeyBounds[K]]] => x }) {

  /**
   * @param metadata RasterMetaData of the layer being queried
   * @param keyBounds Maximum [[KeyBounds]] of the layer
   * @return A sequence of [[KeyBounds]] that cover the queried region
   */
  def apply(metadata: RasterMetaData, keyBounds: KeyBounds[K]): Seq[KeyBounds[K]] = {
    val (_, keyBoundsList) = filterChain((metadata, List(keyBounds)))
    keyBoundsList
  }

  // Allows us to treat Function1 as an instance of a Functor
  import scalaz.Scalaz._

  def where[F, T](exp: RasterRDDFilter.Expression[F, T])(implicit filter: RasterRDDFilter[K,F,T]): RasterRDDQuery[K] = {
    new RasterRDDQuery[K]( {
      filterChain map { case (metadata, keyBoundsList) =>
        val filteredKeyBounds =
          for (keyBound <- keyBoundsList) yield {
            filter(metadata, keyBound, exp)
          }
        (metadata, filteredKeyBounds.flatten)
      }
    })
  }
}

/**
 * Wrapper for [[RasterRDDQuery]] that binds it to some function that is able to produce a [[RasterRDD]].
 */
class BoundRasterRDDQuery[K](query: RasterRDDQuery[K], f: RasterRDDQuery[K] => RasterRDD[K]) {
  def where[F, T](params: RasterRDDFilter.Expression[F, T])(implicit ev: RasterRDDFilter[K,F,T]): BoundRasterRDDQuery[K] =
    new BoundRasterRDDQuery(query.where(params), f)

  def toRDD: RasterRDD[K] = f(query)
}