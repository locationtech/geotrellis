package com.github.mdr.ascii

import com.github.mdr.ascii.impl._

import scala.language.postfixOps

class GraphParserException(message: String) extends RuntimeException(message)

object Diagram {

  @throws(classOf[GraphParserException])
  def apply(s: String): Diagram = new DiagramParse(s).getDiagram

}

sealed trait Container {

  /**
   * @return all the text directly inside this container, excluding any diagram elements (boxes, edges and labels).
   */
  def text: String

  /**
   * @return the area of the diagram covered by this container
   */
  def region: Region

  /**
   * @return all boxes immediately below this container
   */
  def childBoxes: List[Box]

  def parent: Option[Container]

}

trait Diagram extends Container {

  /**
   * @return all boxes in diagram, whether top level or nested
   */
  def allBoxes: List[Box]

  def allEdges: List[Edge]

  def parent: Option[Container] = None

  def boxAt(point: Point): Option[Box]

}

trait Box extends Container {

  /**
   * @return all edges incident to this box
   */
  def edges: List[Edge]

  /**
   * @return edges and other boxes incident to this box, filtered according to the given mode.
   */
  def connections(mode: ConnectMode = ConnectMode.All): List[(Edge, Box)] =
    for {
      edge ← edges
      if mode.includeEdge(edge, this)
      otherBox = edge.otherBox(this)
    } yield edge -> otherBox

}

/**
 * Rule for filtering edges incident to a box
 */
trait ConnectMode {
  def includeEdge(edge: Edge, fromBox: Box): Boolean
}

object ConnectMode {

  /**
   * Return only edges that have an arrow going into the other box
   */
  case object Out extends ConnectMode {
    def includeEdge(edge: Edge, fromBox: Box): Boolean = edge.otherHasArrow(fromBox)
  }

  /**
   * Return only edges that have an arrow coming into this box
   */
  case object In extends ConnectMode {
    def includeEdge(edge: Edge, fromBox: Box): Boolean = edge.hasArrow(fromBox)
  }

  /**
   * Return only undirected edges
   */
  case object Undirected extends ConnectMode {
    def includeEdge(edge: Edge, fromBox: Box): Boolean = edge.hasArrow1 == false && edge.hasArrow2 == false
  }

  /**
   * Return all edges
   */
  case object All extends ConnectMode {
    def includeEdge(edge: Edge, fromBox: Box): Boolean = true
  }

}

trait Edge {

  def points: List[Point]

  def parent: Container

  /**
   * First box incident to this edge
   */
  val box1: Box

  /**
   * Second box incident to this edge
   */
  val box2: Box

  /**
   * First box incident to this edge
   */
  val hasArrow1: Boolean

  val hasArrow2: Boolean

  val label: Option[String]

  /**
   * Given one box in the edge, return the other
   */
  def otherBox(box: Box): Box =
    if (box == box1)
      box2
    else if (box == box2)
      box1
    else
      throw new IllegalArgumentException("Box not part of edge: " + box)

  /**
   * Is there an arrow associated with the given box (which must be part of this edge)
   */
  def hasArrow(box: Box): Boolean =
    if (box == box1)
      hasArrow1
    else if (box == box2)
      hasArrow2
    else
      throw new IllegalArgumentException("Box not part of edge: " + box)

  /**
   * Is there an arrow associated with the other box than the given box (which must be part of this edge)
   */
  def otherHasArrow(box: Box): Boolean = hasArrow(otherBox(box))

}

trait Translatable {

  type Self <: Translatable

  def translate(down: Int = 0, right: Int = 0): Self

  def up: Self = up(1)
  def up(n: Int): Self = translate(down = -n)

  def down: Self = down(1)
  def down(n: Int): Self = translate(down = n)

  def left: Self = left(1)
  def left(n: Int): Self = translate(right = -n)

  def right: Self = right(1)
  def right(n: Int): Self = translate(right = n)

  def go(direction: Direction) = direction match {
    case Up    ⇒ up
    case Down  ⇒ down
    case Left  ⇒ left
    case Right ⇒ right
  }

}

case class Point(row: Int, column: Int) extends Translatable {

  def maxRowCol(that: Point): Point = Point(math.max(this.row, that.row), math.max(this.column, that.column))

  type Self = Point

  def translate(down: Int = 0, right: Int = 0): Point = Point(row + down, column + right)

  def neighbours: List[Point] = List(up, right, down, left)

}

sealed trait Direction {
  val turnLeft: Direction
  val turnRight: Direction
  val opposite: Direction

  def arrow: Char = this match {
    case Up    ⇒ '^'
    case Down  ⇒ 'v'
    case Left  ⇒ '<'
    case Right ⇒ '>'
  }

  def isVertical = this == Up || this == Down

}

case object Up extends Direction {
  val turnLeft = Left
  val turnRight = Right
  val opposite: Direction = Down
}

case object Down extends Direction {
  val turnLeft = Right
  val turnRight = Left
  val opposite: Direction = Up
}

case object Left extends Direction {
  val turnLeft = Down
  val turnRight = Up
  val opposite: Direction = Right
}

case object Right extends Direction {
  val turnLeft = Up
  val turnRight = Down
  val opposite: Direction = Left
}

case class Dimension(height: Int, width: Int)

object Region {

  def apply(topLeft: Point, dimension: Dimension): Region = {
    val bottomRight = Point(topLeft.row + dimension.height - 1, topLeft.column + dimension.width - 1)
    Region(topLeft, bottomRight)
  }

}

case class Region(topLeft: Point, bottomRight: Point) extends Translatable {

  def bottomLeft = Point(bottomRight.row, topLeft.column)

  def topRight = Point(topLeft.row, bottomRight.column)

  def topRow = topLeft.row

  def bottomRow = bottomRight.row

  def leftColumn = topLeft.column

  def rightColumn = bottomRight.column

  def contains(point: Point): Boolean = {
    point.row >= topRow && point.column >= leftColumn &&
      point.row <= bottomRow && point.column <= rightColumn
  }

  def contains(region: Region): Boolean = contains(region.topLeft) && contains(region.bottomRight)

  def intersects(region: Region): Boolean = {
    val disjoint =
      this.rightColumn < region.leftColumn ||
        region.rightColumn < this.leftColumn ||
        this.bottomRow < region.topRow ||
        region.bottomRow < this.topRow
    !disjoint
  }

  def width = rightColumn - leftColumn + 1

  def height = bottomRow - topRow + 1

  def dimension = Dimension(height, width)

  def area = width * height

  def points: List[Point] =
    for {
      row ← (topRow to bottomRow toList)
      column ← leftColumn to rightColumn
    } yield Point(row, column)

  type Self = Region

  def translate(down: Int = 0, right: Int = 0): Region =
    Region(topLeft.translate(down, right), bottomRight.translate(down, right))

}
