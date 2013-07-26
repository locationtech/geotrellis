package com.github.mdr.ascii.layout

import com.github.mdr.ascii.util.Utils

object LayerOrderingCalculator {

  def reorder(layering: Layering): Layering = {
    var previousLayerOpt: Option[Layer] = None
    val newLayers = layering.layers.map { currentLayer ⇒
      val updatedLayer = previousLayerOpt match {
        case Some(previousLayer) ⇒ reorder(previousLayer, currentLayer, layering.edges)
        case None                ⇒ currentLayer
      }
      previousLayerOpt = Some(updatedLayer)
      updatedLayer
    }
    layering.copy(layers = newLayers)
  }

  def reorder(layer1: Layer, layer2: Layer, edges: List[Edge]): Layer = {
    def inVertices(vertex: Vertex): List[Vertex] = edges.collect { case Edge(v1, `vertex`) ⇒ v1 }
    def barycenter(vertex: Vertex): Double = {
      val in = inVertices(vertex)
      in.map(v ⇒ layer1.vertices.indexOf(v).ensuring(_ >= 0)).sum.toDouble / in.size
    }
    val reorderedVertices = layer2.vertices.sortBy(barycenter)
    layer2.copy(vertices = reorderedVertices)
  }

}