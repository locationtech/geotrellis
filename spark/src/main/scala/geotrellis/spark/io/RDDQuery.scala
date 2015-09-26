
package geotrellis.spark.io

import geotrellis.spark.{Boundable, KeyBounds}

/**
 * Accumulation of [[RasterRDDFilter]]s that will be asked to filter layer [[KeyBounds]]
 *
 * @tparam K  Type of key for the RDD being filtered
 * @tparam M  Type of metadata used for filtering

 */
class RDDQuery[K: Boundable, M](
  filterChain: ( (M, List[KeyBounds[K]]) ) => (M, List[KeyBounds[K]]) = { x: (M, List[KeyBounds[K]]) => x }) {

  /**
   * @param metadata RasterMetaData of the layer being queried
   * @param keyBounds Maximum [[KeyBounds]] of the layer
   * @return A sequence of [[KeyBounds]] that cover the queried region
   */
  def apply(metadata: M, keyBounds: KeyBounds[K]): Seq[KeyBounds[K]] = {
    val (_, keyBoundsList) = filterChain((metadata, List(keyBounds)))
    keyBoundsList
  }

  // Allows us to treat Function1 as an instance of a Functor
  import scalaz.Scalaz._

  def where[F, T](exp: RDDFilter.Expression[F, T])(implicit filter: RDDFilter[K,F,T, M]): RDDQuery[K, M] = {
    new RDDQuery( {
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
 * Wrapper for [[RDDQuery]] that binds it to some function that is able to produce a [[RasterRDD]].
 */
class BoundRDDQuery[K, M, ReturnType](query: RDDQuery[K, M], f: RDDQuery[K, M] => ReturnType) {
  def where[F, T](params: RDDFilter.Expression[F, T])(implicit ev: RDDFilter[K,F,T, M]): BoundRDDQuery[K, M, ReturnType] =
    new BoundRDDQuery(query.where(params), f)

  def toRDD: ReturnType = f(query)
}