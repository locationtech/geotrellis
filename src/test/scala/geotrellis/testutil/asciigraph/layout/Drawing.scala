package com.github.mdr.ascii.layout

import com.github.mdr.ascii.Dimension
import com.github.mdr.ascii.Point

case class Drawing(elements: List[DrawingElement]) {

  def dimension: Dimension = {
    val largestPoint =
      elements.flatMap {
        case element: VertexDrawingElement ⇒ List(element.region.bottomRight)
        case element: EdgeDrawingElement   ⇒ element.bendPoints
      }.foldLeft(Point(0, 0))(_ maxRowCol _)
    Dimension(width = largestPoint.column + 1, height = largestPoint.row + 1)
  }

  def replaceElement(element: DrawingElement, replacement: DrawingElement) =
    copy(elements = replacement :: elements.filterNot(_ == element))

  def vertexElementAt(point: Point): Option[VertexDrawingElement] =
    elements.collect {
      case vde: VertexDrawingElement if vde.region.contains(point) ⇒ vde
    }.headOption

}

