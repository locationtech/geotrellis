package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.util._

/**
  * Accumulation of [[LayerFilter]]s that will be asked to filter layer [[KeyBounds]]
  *
  * @tparam K  Type of key for the RDD being filtered
  * @tparam M  Type of metadata used for filtering
  */
class LayerQuery[K: Boundable, M: GetComponent[?, Bounds[K]]](
  filterChain: ( (M, List[KeyBounds[K]]) ) => (M, List[KeyBounds[K]]) = { x: (M, List[KeyBounds[K]]) => x }) {

  /**
    * @param metadata RasterMetaData of the layer being queried
    * @return A sequence of [[KeyBounds]] that cover the queried region
    */
  def apply(metadata: M): Seq[KeyBounds[K]] =
    metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        val (_, keyBoundsList) = filterChain((metadata, List(keyBounds)))
        keyBoundsList
      case EmptyBounds =>
        Seq()
    }

  // Allows us to treat Function1 as an instance of a Functor
  import scalaz.Scalaz._

  def where[F, T](exp: LayerFilter.Expression[F, T])(implicit filter: LayerFilter[K, F, T, M]): LayerQuery[K, M] = {
    new LayerQuery( {
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
  * Wrapper for [[LayerQuery]] that binds it to some function that is able to produce a resulting value.
  */
class BoundLayerQuery[K, M, ReturnType](query: LayerQuery[K, M], f: LayerQuery[K, M] => ReturnType) {
  def where[F, T](params: LayerFilter.Expression[F, T])(implicit ev: LayerFilter[K, F, T, M]): BoundLayerQuery[K, M, ReturnType] =
    new BoundLayerQuery(query.where(params), f)

  def result: ReturnType = f(query)
}
