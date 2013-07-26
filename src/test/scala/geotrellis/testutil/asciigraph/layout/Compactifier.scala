package com.github.mdr.ascii.layout

import com.github.mdr.ascii.util.Utils._
import com.github.mdr.ascii.Down
import com.github.mdr.ascii.Point

object Compactifier {

  def compactify(drawing: Drawing): Drawing = removeRedundantRows(elevateEdges(drawing))

  private def elevateEdges(drawing: Drawing): Drawing = iterate(drawing, elevateEdge)

  private def elevateEdge(drawing: Drawing): Option[Drawing] = {
    val grid = new OccupancyGrid(drawing)
    for {
      edgeElement ← drawing.elements.collect { case ede: EdgeDrawingElement ⇒ ede }
      updatedElement ← elevateEdge(edgeElement, grid)
    } {
      //      println("Shifted edge up in " + edgeElement + ", " + updatedElement)
      val updatedDrawing = drawing.replaceElement(edgeElement, updatedElement)
      return Some(updatedDrawing)
    }
    None
  }

  private def elevateEdge(edgeElement: EdgeDrawingElement, grid: OccupancyGrid): Option[EdgeDrawingElement] = {
    for {
      (segment1, segment2, segment3) ← adjacentTriples(edgeElement.segments)
      if segment1.direction == Down && segment3.direction == Down
      row ← (segment1.start.row + /* 2 */ 1) to (segment2.start.row - 1)
    } {
      val alternativeStart2 = segment2.start.copy(row = row)
      val alternativeFinish2 = segment2.finish.copy(row = row)
      val fakeElement = new EdgeDrawingElement(
        List(segment1.start, alternativeStart2, alternativeFinish2, segment3.finish), false, false)
      val newPoints = fakeElement.points.filterNot(edgeElement.points.contains)
      val allClear = !newPoints.exists(grid.isOccupied)
      if (allClear) {
        val oldBendPoints = edgeElement.bendPoints
        val oldIndex = oldBendPoints.indexOf(segment2.start).ensuring(_ >= 0)
        val newBendPoints = oldBendPoints.patch(oldIndex, List(alternativeStart2, alternativeFinish2), 2)
        val updated = edgeElement.copy(bendPoints = newBendPoints)
        return Some(updated)
      }

    }
    None
  }

  private def removeRedundantRows(drawing: Drawing): Drawing = iterate(drawing, removeRedundantRow)

  private def removeRedundantRow(drawing: Drawing): Option[Drawing] = {
    for (row ← 0 until drawing.dimension.height if canRemove(drawing, row))
      return Some(removeRows(drawing, row, row))
    None
  }

  private def canRemove(drawing: Drawing, row: Int): Boolean =
    drawing.elements.forall {
      case ede: EdgeDrawingElement   ⇒ canRemove(ede, row)
      case vde: VertexDrawingElement ⇒ row < vde.region.topRow || row > vde.region.bottomRow
    }

  private def canRemove(ede: EdgeDrawingElement, row: Int): Boolean = {
    val List(firstBendPoint, secondBendPoint, _*) = ede.bendPoints
    val wouldLeaveStubbyUpArrow =
      row == firstBendPoint.row + 1 &&
        ede.hasArrow1 &&
        secondBendPoint.row == row + 1 &&
        ede.bendPoints.size > 2

    val List(lastBendPoint, secondLastBendPoint, _*) = ede.bendPoints.reverse
    val wouldLeaveStubbyDownArrow =
      row == lastBendPoint.row - 1 &&
        ede.hasArrow2 &&
        secondLastBendPoint.row == row - 1 &&
        ede.bendPoints.size > 2

    !wouldLeaveStubbyDownArrow && !wouldLeaveStubbyUpArrow && ede.bendPoints.forall(_.row != row)
  }

  private def removeRows(drawing: Drawing, fromRow: Int, toRow: Int): Drawing = {
    val rowsToRemove = (toRow - fromRow + 1).ensuring(_ >= 0)
    val newElements = drawing.elements map {
      case ede: EdgeDrawingElement ⇒
        val newBendPoints = ede.bendPoints.map { point ⇒
          if (point.row < fromRow)
            point
          else
            point.up(rowsToRemove)
        }
        ede.copy(bendPoints = newBendPoints)
      case vde: VertexDrawingElement ⇒
        if (vde.region.topRow < fromRow)
          vde
        else
          vde.up(rowsToRemove)
    }
    drawing.copy(elements = newElements)
  }

}