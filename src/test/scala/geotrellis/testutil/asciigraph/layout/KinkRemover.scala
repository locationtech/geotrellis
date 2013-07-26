package com.github.mdr.ascii.layout

import com.github.mdr.ascii.Dimension
import com.github.mdr.ascii.Point
import com.github.mdr.ascii.util.Utils._
import com.github.mdr.ascii.Direction
import com.github.mdr.ascii._

object KinkRemover {

  def removeKinks(drawing: Drawing): Drawing = iterate(drawing, removeKink)

  private def removeKink(drawing: Drawing): Option[Drawing] = {
    val grid = new OccupancyGrid(drawing)
    for {
      edgeElement ← drawing.elements.collect { case ede: EdgeDrawingElement ⇒ ede }
      updatedElement ← removeKink(edgeElement, drawing, grid)
    } {
      //      println("Removed kink in " + edgeElement + ", " + updatedElement)
      return Some(drawing.replaceElement(edgeElement, updatedElement))
    }
    None
  }

  private def removeStraightLines(points: List[Point]): List[Point] = points match {
    case Nil | List(_) | List(_, _) ⇒ points
    case (p1 @ Point(r1, c1)) :: Point(r2, c2) :: (p3 @ Point(r3, c3)) :: remainder if c1 == c2 && c2 == c3 || r1 == r2 && r2 == r3 ⇒
      removeStraightLines(p1 :: p3 :: remainder)
    case p :: ps ⇒ p :: removeStraightLines(ps)
  }

  private def removeKink(element: EdgeDrawingElement, drawing: Drawing, grid: OccupancyGrid): Option[EdgeDrawingElement] = {
    val segments: List[EdgeSegment] = element.segments
    adjacentPairs(segments) collect {
      case (segment1 @ EdgeSegment(start, Down, middle), segment2 @ EdgeSegment(_, Left | Right, end)) ⇒
        val alternativeMiddle = Point(start.row, end.column)
        val fakeElement = new EdgeDrawingElement(List(start, alternativeMiddle, end), false, false)
        val newPoints = fakeElement.points.filterNot(element.points.contains)
        val allClear = !newPoints.exists(grid.isOccupied)
        if (allClear) {
          val originalStartVertex = drawing.vertexElementAt(start)
          val newStartVertex = drawing.vertexElementAt(alternativeMiddle)
          if (segment1 != element.segments.head ||
            drawing.vertexElementAt(start.up) == drawing.vertexElementAt(alternativeMiddle.up) &&
            drawing.vertexElementAt(start.up).get.region.rightColumn != alternativeMiddle.column &&
            drawing.vertexElementAt(start.up).get.region.leftColumn != alternativeMiddle.column) {
            val oldBendPoints = element.bendPoints
            val oldIndex = oldBendPoints.indexOf(start).ensuring(_ >= 0)
            val newBendPoints = removeStraightLines(oldBendPoints.patch(oldIndex, List(alternativeMiddle), 3).distinct)
            val updated = element.copy(bendPoints = newBendPoints)
            return Some(updated)
          }
        }
      case (segment1 @ EdgeSegment(start, Left | Right, middle), segment2 @ EdgeSegment(_, Down, end)) ⇒
        val alternativeMiddle = Point(end.row, start.column)
        val fakeElement = new EdgeDrawingElement(List(start, alternativeMiddle, end), false, false)
        val newPoints = fakeElement.points.filterNot(element.points.contains)
        val allClear = !newPoints.exists(grid.isOccupied)
        if (allClear) {
          val originalStartVertex = drawing.vertexElementAt(end)
          val newStartVertex = drawing.vertexElementAt(alternativeMiddle)
          if (segment2 != element.segments.last ||
            drawing.vertexElementAt(end.down) == drawing.vertexElementAt(alternativeMiddle.down) &&
            drawing.vertexElementAt(end.down).get.region.rightColumn != alternativeMiddle.column &&
            drawing.vertexElementAt(end.down).get.region.leftColumn != alternativeMiddle.column) {
            val oldBendPoints = element.bendPoints
            val oldIndex = oldBendPoints.indexOf(start).ensuring(_ >= 0)
            val newBendPoints = removeStraightLines(oldBendPoints.patch(oldIndex, List(alternativeMiddle), 3).distinct)
            val updated = element.copy(bendPoints = newBendPoints)
            return Some(updated)
          }
        }
    }
    None
  }

}

class OccupancyGrid(drawing: Drawing) {

  private val grid: Array[Array[Boolean]] = Array.fill(drawing.dimension.height, drawing.dimension.width)(false)

  drawing.elements.foreach(record)

  def apply(point: Point): Boolean = grid(point.row)(point.column)

  def isOccupied(point: Point) = this(point)

  private def record(drawingElement: DrawingElement) = drawingElement.points.foreach(markAsOccupied)

  private def markAsOccupied(point: Point) {
    grid(point.row)(point.column) = true
  }

}

