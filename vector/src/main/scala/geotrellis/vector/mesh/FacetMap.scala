package geotrellis.vector.mesh

trait FacetMap[HE <: HalfEdge, F] {
  def +=(keyValue: (F, HE)): Unit
  //def +=(edge: HE)(implicit nav: mesh.HalfEdgeNavigation[HE, V, F])
  def -=(idx: F): Unit
  //def -=(edge: HE)
  def facets(): Map[F, HE]
}
