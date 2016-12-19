package geotrellis.vector.mesh

trait HalfEdgeMesh[E <: HalfEdge, V, F] {
  val boundary: Option[E]
  val edgeIncidentToVertex: Map[V, E]
  val facets: FacetMap[E, F]
}
