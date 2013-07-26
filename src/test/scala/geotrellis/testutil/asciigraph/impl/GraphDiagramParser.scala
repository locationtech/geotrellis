package com.github.mdr.ascii.impl

import com.github.mdr.ascii._
import scala.annotation.tailrec
import scala.language.postfixOps

class DiagramParse(s: String) {

  private val rawRows: List[String] = if (s.isEmpty) Nil else s.split("(\r)?\n").toList

  private val numberOfColumns = rawRows.map(_.length).max

  private val rows = rawRows.map(_.padTo(numberOfColumns, ' ')).toArray

  private val numberOfRows = rows.length

  private def charAt(point: Point): Char = rows(point.row)(point.column)
  private def charAtOpt(point: Point): Option[Char] = if (inDiagram(point)) Some(charAt(point)) else None

  private def inDiagram(point: Point) = point match {
    case Point(row, column) ⇒
      row >= 0 && column >= 0 && row < numberOfRows && column < numberOfColumns
  }

  @tailrec
  private def scanBoxEdgeRight(start: Point): Option[Point] = charAt(start) match {
    case '+'                           ⇒ Some(start)
    case '-' if inDiagram(start.right) ⇒ scanBoxEdgeRight(start.right)
    case _                             ⇒ None
  }

  @tailrec
  private def scanBoxEdgeDown(start: Point): Option[Point] = charAt(start) match {
    case '+'                          ⇒ Some(start)
    case '|' if inDiagram(start.down) ⇒ scanBoxEdgeDown(start.down)
    case _                            ⇒ None
  }

  /**
   * @return bottomRight of box if all edges are correct
   */
  private def completeBox(topLeft: Point): Option[Point] =
    for {
      topRight ← scanBoxEdgeRight(topLeft.right)
      bottomRight ← scanBoxEdgeDown(topRight.down)
      bottomLeft ← scanBoxEdgeDown(topLeft.down)
      bottomRight2 ← scanBoxEdgeRight(bottomLeft.right)
      if bottomRight == bottomRight2
    } yield bottomRight

  private val possibleTopLefts: List[Point] =
    for {
      row ← (0 until numberOfRows - 1).toList
      column ← 0 until numberOfColumns - 1
      point = Point(row, column)
      if charAt(point) == '+'
      if charAt(point.right) == '-'
      if charAt(point.down) == '|'
    } yield point

  private val allBoxes =
    for {
      topLeft ← possibleTopLefts
      bottomRight ← completeBox(topLeft)
    } yield new BoxImpl(topLeft, bottomRight)

  def getDiagram: Diagram = diagram

  private val diagram = new DiagramImpl(numberOfRows, numberOfColumns)

  diagram.allBoxes = allBoxes

  private val boxContains: Map[BoxImpl, BoxImpl] =
    (for {
      outerBox ← allBoxes
      innerBox ← allBoxes
      if outerBox != innerBox
      if outerBox.region contains innerBox.region
    } yield outerBox -> innerBox).toMap

  for {
    (box, containingBoxMap) ← boxContains.groupBy(_._2)
    containingBoxes = box :: containingBoxMap.keys.toList
    orderedBoxes = containingBoxes.sortBy(_.region.area)
    (childBox, parentBox) ← orderedBoxes.zip(orderedBoxes drop 1)
  } {
    childBox.parent = Some(parentBox)
    parentBox.childBoxes ::= childBox
  }

  for (box ← allBoxes if box.parent.isEmpty) {
    diagram.childBoxes ::= box
    box.parent = Some(diagram)
  }

  @tailrec
  private def followEdge(direction: Direction, edgeSoFar: List[Point]): Option[EdgeImpl] = {
    val currentPoint = edgeSoFar.head
    if (!inDiagram(currentPoint))
      return None
    def isBoxEdge(point: Point) = inDiagram(point) && allBoxes.exists(_.boundaryPoints.contains(point))
    def finaliseEdge(connectPoint: Point): Option[EdgeImpl] = {
      val points = (connectPoint :: edgeSoFar).reverse
      if (points.size <= 2)
        None
      else
        Some(new EdgeImpl(points))
    }
    val ahead: Point = currentPoint.go(direction)

    def isEdgeChar(c: Char) = c match {
      case '-' | '+' | 'v' | 'V' | '^' | '>' | '<' | '|' ⇒ true
      case _                                             ⇒ false
    }

    if (direction == Left || direction == Right)
      (charAtOpt(ahead), charAtOpt(ahead.go(Up)), charAtOpt(ahead.go(Down)), charAtOpt(ahead.go(direction))) match {
        case _ if isBoxEdge(ahead)                                  ⇒ finaliseEdge(ahead)
        case (Some('+'), _, _, _)                                   ⇒ followEdge(direction, ahead :: edgeSoFar)
        case (Some('v' | 'V'), _, _, _)                             ⇒ followEdge(Down, ahead :: edgeSoFar)
        case (Some('^'), _, _, _)                                   ⇒ followEdge(Up, ahead :: edgeSoFar)
        case (Some('-'), _, _, Some(c)) if isEdgeChar(c)            ⇒ followEdge(direction, ahead :: edgeSoFar)
        case (Some('-' | '|'), Some('^'), Some('v' | 'V'), _)       ⇒ throw new GraphParserException("Ambiguous turn at " + ahead)
        case (Some('-' | '|'), Some('^'), _, _)                     ⇒ followEdge(Up, ahead :: edgeSoFar)
        case (Some('-' | '|'), _, Some('v' | 'V'), _)               ⇒ followEdge(Down, ahead :: edgeSoFar)
        case (Some('-' | '|'), Some('|' | '+'), Some('|' | '+'), _) ⇒ throw new GraphParserException("Ambiguous turn at " + ahead)
        case (Some('-' | '|'), Some('|' | '+'), _, _)               ⇒ followEdge(Up, ahead :: edgeSoFar)
        case (Some('-' | '|'), _, Some('|' | '+'), _)               ⇒ followEdge(Down, ahead :: edgeSoFar)
        case (Some('|'), Some('-'), Some('-'), _)                   ⇒ throw new GraphParserException("Ambiguous turn at " + ahead)
        case (Some('|'), Some('-'), _, _)                           ⇒ followEdge(Up, ahead :: edgeSoFar)
        case (Some('|'), _, Some('-'), _)                           ⇒ followEdge(Down, ahead :: edgeSoFar)
        case (Some('<'), _, _, _) if direction == Left              ⇒ followEdge(direction, ahead :: edgeSoFar)
        case (Some('>'), _, _, _) if direction == Right             ⇒ followEdge(direction, ahead :: edgeSoFar)
        case _                                                      ⇒ None
      }
    else
      (charAtOpt(ahead), charAtOpt(ahead.go(Left)), charAtOpt(ahead.go(Right)), charAtOpt(ahead.go(direction))) match {
        case _ if isBoxEdge(ahead)                                  ⇒ finaliseEdge(ahead)
        case (Some('+'), _, _, _)                                   ⇒ followEdge(direction, ahead :: edgeSoFar)
        case (Some('<'), _, _, _)                                   ⇒ followEdge(Left, ahead :: edgeSoFar)
        case (Some('>'), _, _, _)                                   ⇒ followEdge(Right, ahead :: edgeSoFar)
        case (Some('|'), _, _, Some(c)) if isEdgeChar(c)            ⇒ followEdge(direction, ahead :: edgeSoFar)
        case (Some('-' | '|'), Some('<'), Some('>'), _)             ⇒ throw new RuntimeException("Ambiguous turn at " + ahead)
        case (Some('-' | '|'), Some('<'), _, _)                     ⇒ followEdge(Left, ahead :: edgeSoFar)
        case (Some('-' | '|'), _, Some('>'), _)                     ⇒ followEdge(Right, ahead :: edgeSoFar)
        case (Some('-' | '|'), Some('-' | '+'), Some('-' | '+'), _) ⇒ throw new RuntimeException("Ambiguous turn at " + ahead)
        case (Some('-' | '|'), Some('-' | '+'), _, _)               ⇒ followEdge(Left, ahead :: edgeSoFar)
        case (Some('-' | '|'), _, Some('-' | '+'), _)               ⇒ followEdge(Right, ahead :: edgeSoFar)
        case (Some('-'), Some('|'), Some('|'), _)                   ⇒ throw new RuntimeException("Ambiguous turn at " + ahead)
        case (Some('-'), Some('|'), _, _)                           ⇒ followEdge(Left, ahead :: edgeSoFar)
        case (Some('-'), _, Some('|'), _)                           ⇒ followEdge(Right, ahead :: edgeSoFar)
        case (Some('^'), _, _, _) if direction == Up                ⇒ followEdge(direction, ahead :: edgeSoFar)
        case (Some('V' | 'v'), _, _, _) if direction == Down        ⇒ followEdge(direction, ahead :: edgeSoFar)
        case _                                                      ⇒ None
      }
  }

  private def followEdge(direction: Direction, startPoint: Point): Option[EdgeImpl] =
    followEdge(direction, startPoint :: Nil)

  private val edges =
    allBoxes.flatMap { box ⇒
      box.rightBoundary.flatMap(followEdge(Right, _)) ++
        box.leftBoundary.flatMap(followEdge(Left, _)) ++
        box.topBoundary.flatMap(followEdge(Up, _)) ++
        box.bottomBoundary.flatMap(followEdge(Down, _))
    }

  diagram.allEdges = edges.groupBy(_.points.toSet).values.toList.map(_.head)
  for (edge ← diagram.allEdges) {
    edge.box1.edges ::= edge
    if (edge.box1 != edge.box2)
      edge.box2.edges ::= edge
  }

  val allEdgePoints = diagram.allEdges.flatMap(_.points)

  //  for {
  //    box1 ← allBoxes
  //    box2 ← allBoxes
  //    if box1 != box2
  //    if box1.region.intersects(box2.region)
  //  } throw new GraphParserException("Overlapping boxes: " + box1 + box2)

  def collectText(container: ContainerImpl): String = {
    val region = container.contentsRegion
    val allPoints = (container.childBoxes.flatMap(_.region.points) ++ allEdgePoints ++ allLabelPoints).toSet
    val sb = new StringBuilder
    for (row ← region.topLeft.row to region.bottomRight.row) {
      for {
        column ← region.topLeft.column to region.bottomRight.column
        point = Point(row, column)
        if !allPoints.contains(point)
        c = charAt(point)
      } sb.append(c)
      sb.append("\n")
    }
    sb.deleteCharAt(sb.length - 1)
    sb.toString
  }

  for (edge ← diagram.allEdges) {
    val labels: Set[Label] =
      (for {
        point ← edge.points
        startPoint ← point.neighbours
        ('[' | ']') ← charAtOpt(startPoint)
        label ← completeLabel(startPoint, edge.parent)
      } yield label).toSet
    if (labels.size > 1)
      throw new GraphParserException("Multiple labels for edge " + edge + ", " + labels.map(_.text).mkString(","))
    edge.label_ = labels.headOption
  }

  private lazy val allLabelPoints: Set[Point] =
    (for {
      edge ← diagram.allEdges
      label ← edge.label_.toList
      point ← label.points
    } yield point).toSet

  for (box ← allBoxes)
    box.text = collectText(box)
  diagram.text = collectText(diagram)

  private def completeLabel(startPoint: Point, parent: ContainerImpl): Option[Label] = {
    val occupiedPoints = parent.childBoxes.flatMap(_.region.points) ++ allEdgePoints toSet
    val (finalChar, direction) = charAt(startPoint) match {
      case '[' ⇒ (']', Right)
      case ']' ⇒ ('[', Left)
    }

    def search(point: Point): Option[Label] = charAtOpt(point) flatMap {
      case `finalChar` ⇒
        val List(p1, p2) = List(startPoint, point).sortBy(_.column)
        Some(Label(p1, p2))
      case _ if occupiedPoints.contains(point) ⇒
        None
      case _ ⇒
        search(point.go(direction))
    }

    search(startPoint.go(direction))
  }

  private abstract class ContainerImpl extends RegionToString { self: Container ⇒

    var text: String = ""

    var childBoxes: List[BoxImpl] = Nil

    def contentsRegion: Region

  }

  private class BoxImpl(val topLeft: Point, val bottomRight: Point) extends ContainerImpl with Box {

    var edges: List[EdgeImpl] = Nil

    var parent: Option[Container with ContainerImpl] = None

    def region: Region = Region(topLeft, bottomRight)

    def contentsRegion: Region = Region(topLeft.right.down, bottomRight.up.left)

    val leftBoundary: List[Point] = for (row ← topLeft.row to bottomRight.row toList) yield Point(row, topLeft.column)
    val rightBoundary: List[Point] = for (row ← topLeft.row to bottomRight.row toList) yield Point(row, bottomRight.column)
    val topBoundary: List[Point] = for (column ← topLeft.column to bottomRight.column toList) yield Point(topLeft.row, column)
    val bottomBoundary: List[Point] = for (column ← topLeft.column to bottomRight.column toList) yield Point(bottomRight.row, column)

    val boundaryPoints: Set[Point] = leftBoundary.toSet ++ rightBoundary.toSet ++ topBoundary.toSet ++ bottomBoundary.toSet

  }

  private def isArrow(c: Char) = c match {
    case '^' | 'v' | 'V' | '>' | '<' ⇒ true
    case _                           ⇒ false
  }

  private class EdgeImpl(val points: List[Point]) extends Edge {

    val box1: BoxImpl = diagram.boxAt(points.head).get

    val box2: BoxImpl = diagram.boxAt(points.last).get

    var label_ : Option[Label] = None

    lazy val label = label_.map(_.text)

    lazy val parent: Container with ContainerImpl = if (box1.parent == Some(box2)) box2 else box2.parent.get

    lazy val hasArrow1 = isArrow(charAt(points.drop(1).head))

    lazy val hasArrow2 = isArrow(charAt(points.dropRight(1).last))

    lazy val edgeAndLabelPoints: List[Point] = points ++ label_.map(_.points).getOrElse(Nil)

    override def toString = diagramRegionToString(region(edgeAndLabelPoints), edgeAndLabelPoints.contains)

    def region(points: List[Point]): Region =
      Region(
        Point(
          points.map(_.row).min,
          points.map(_.column).min),
        Point(
          points.map(_.row).max,
          points.map(_.column).max))

  }

  private trait RegionToString {

    def region: Region

    override def toString = diagramRegionToString(region)

  }

  def diagramRegionToString(region: Region, includePoint: Point ⇒ Boolean = p ⇒ true) = {
    val sb = new StringBuilder("\n")
    for (row ← region.topLeft.row to region.bottomRight.row) {
      for {
        column ← region.topLeft.column to region.bottomRight.column
        point = Point(row, column)
        c = if (includePoint(point)) charAt(point) else ' '
      } sb.append(c)
      sb.append("\n")
    }
    sb.toString
  }

  private class DiagramImpl(numberOfRows: Int, numberOfColumns: Int) extends ContainerImpl with Diagram {

    var allBoxes: List[BoxImpl] = Nil

    var allEdges: List[EdgeImpl] = Nil

    def boxAt(point: Point): Option[BoxImpl] = allBoxes.find(_.boundaryPoints.contains(point))

    def region: Region = Region(Point(0, 0), Point(numberOfRows - 1, numberOfColumns - 1))

    def contentsRegion = region

  }

  private case class Label(start: Point, end: Point) {

    require(start.row == end.row)

    val row = start.row

    def points: List[Point] =
      for (column ← start.column to end.column toList)
        yield Point(row, column)

    val text: String = {
      val sb = new StringBuilder
      for (column ← start.column + 1 to end.column - 1)
        sb.append(charAt(Point(row, column)))
      sb.toString
    }

  }

}

