package com.github.mdr.ascii.layout

import com.github.mdr.ascii._
import com.github.mdr.ascii.util.Utils

object Layouter {

  def renderGraph[T](graph: layout.Graph[T]): String = {
    val (newGraph, reversedEdges) = new CycleRemover[T].removeCycles(graph)
    val layering = new LayeringCalculator[T].assignLayers(newGraph, reversedEdges.toSet)
    val layouter = new Layouter[Int](ToStringVertexRenderingStrategy)
    val drawing = layouter.layout(LayerOrderingCalculator.reorder(layering))
    val cleanedUpDrawing = Compactifier.compactify(KinkRemover.removeKinks(drawing))
    Renderer.render(cleanedUpDrawing)
  }

}

class Layouter[V](vertexRenderingStrategy: VertexRenderingStrategy[V]) {

  private case class VertexInfo(region: Region, inPorts: Map[Edge, Point], outPorts: Map[Edge, Point]) {

    def contentRegion: Region = region.copy(topLeft = region.topLeft.down.right, bottomRight = region.bottomRight.up.left)

    def translate(down: Int = 0, right: Int = 0): VertexInfo = {
      val newRegion = region.translate(down, right)
      VertexInfo(
        region.translate(down, right),
        Utils.transformValues(inPorts)(_.translate(down, right)),
        Utils.transformValues(outPorts)(_.translate(down, right)))
    }

    def setLeft(column: Int): VertexInfo = translate(right = column - region.leftColumn)

  }

  val VERTEX_HEIGHT = 3

  private def calculateVertexInfo(layer: Layer, edges: List[Edge], previousLayerOpt: Option[Layer], nextLayerOpt: Option[Layer]): LayerVertexInfos = {
    val inEdges = previousLayerOpt.map { previousLayer ⇒
      edges.sortBy { case Edge(v1, _) ⇒ previousLayer.vertices.indexOf(v1) }
    }.getOrElse(Nil)
    val outEdges = nextLayerOpt.map { nextLayer ⇒
      edges.sortBy { case Edge(_, v2) ⇒ nextLayer.vertices.indexOf(v2) }
    }.getOrElse(Nil)
    def inVertices(vertex: Vertex) = inEdges collect { case e @ Edge(v1, `vertex`) ⇒ e }
    def outVertices(vertex: Vertex) = outEdges collect { case e @ Edge(`vertex`, v2) ⇒ e }

    val dimensions: Map[Vertex, Dimension] =
      (for {
        vertex ← layer.vertices
        outDegree = outVertices(vertex).size
        inDegree = inVertices(vertex).size
      } yield {
        val dimension = vertex match {
          case realVertex: RealVertex ⇒
            val Dimension(preferredHeight, preferredWidth) =
              vertexRenderingStrategy.getPreferredSize(realVertex.contents.asInstanceOf[V])
            val width = math.max(math.max(outDegree * 2 + 3, inDegree * 2 + 3), preferredWidth + 2)
            val height = math.max(VERTEX_HEIGHT, preferredHeight + 2)
            Dimension(height = height, width = width)
          case _: DummyVertex ⇒
            Dimension(height = 1, width = 1)
        }
        vertex -> dimension
      }).toMap

    var regions: Map[Vertex, Region] = Map()
    var pos = Point(0, 0)
    for (vertex ← layer.vertices) {
      val region = Region(pos, dimensions(vertex))
      regions += vertex -> region
      pos = region.topRight.right(2)
    }

    def spacePorts(edges: List[Edge], vertexWidth: Int): List[(Edge, Int)] = {
      val factor = vertexWidth / (edges.size + 1)
      val centraliser = (vertexWidth - factor * (edges.size + 1)) / 2
      edges.zipWithIndex.map { case (v, i) ⇒ (v, (i + 1) * factor + centraliser) }
    }

    def makeVertexInfo(vertex: Vertex): VertexInfo = {
      val region = regions(vertex)
      vertex match {
        case _: RealVertex ⇒
          val inPorts = (for ((edge, offset) ← spacePorts(inVertices(vertex), region.width))
            yield edge -> region.topLeft.right(offset)).toMap
          val outPorts = (for ((edge, offset) ← spacePorts(outVertices(vertex), region.width))
            yield edge -> region.bottomLeft.right(offset)).toMap
          VertexInfo(region, inPorts, outPorts)
        case _: DummyVertex ⇒
          val List(inVertex) = inVertices(vertex)
          val List(outVertex) = outVertices(vertex)
          VertexInfo(region, Map(inVertex -> region.topLeft), Map(outVertex -> region.topLeft))
      }
    }

    LayerVertexInfos((for (vertex ← layer.vertices) yield vertex -> makeVertexInfo(vertex)).toMap)
  }

  case class EdgeInfo(startVertex: Vertex, finishVertex: Vertex, startPort: Point, finishPort: Point, reversed: Boolean)

  private def calculateEdgeOrdering(edgeInfos: List[EdgeInfo]): Map[EdgeInfo, Int] = {

    // We sort this way to avoid unnecessary overlaps coming into the same vertex
    val sortedInfos = edgeInfos.sortBy { info ⇒
      val diff = info.startPort.column - info.finishPort.column
      val sign = if (diff == 0) 0 else diff / math.abs(diff)
      sign * info.finishPort.column
    }

    var edgeRows: Map[EdgeInfo, Int] = Map()
    var rowNumber = 0
    for { edgeInfo @ EdgeInfo(_, _, startPort, finishPort, _) ← sortedInfos } {
      if (startPort.column != finishPort.column) {
        edgeRows += edgeInfo -> rowNumber
        rowNumber += 1
      }
    }

    // Force edges that share start and end columns to be ordered correctly to avoid conflicts
    var continue = true
    while (continue) {
      continue = false
      for {
        edgeInfo1 @ EdgeInfo(_, _, start1, _, _) ← sortedInfos
        edgeInfo2 @ EdgeInfo(_, _, _, finish2, _) ← sortedInfos
        if edgeInfo1 != edgeInfo2
        if start1.column == finish2.column
        row1 = edgeRows(edgeInfo1)
        row2 = edgeRows(edgeInfo2)
        if row1 > row2
      } {
        edgeRows += edgeInfo1 -> row2
        edgeRows += edgeInfo2 -> row1
        continue = true
      }
    }

    edgeRows
  }

  private case class RowLayoutResult(
    drawingElements: List[DrawingElement],
    updatedVertexInfos: LayerVertexInfos,
    updatedIncompletedEdges: Map[DummyVertex, List[Point]])

  private def layoutRow(
    vertexInfos1: LayerVertexInfos,
    vertexInfos2: LayerVertexInfos,
    edges: List[Edge],
    incompleteEdges: Map[DummyVertex, List[Point]]): RowLayoutResult = {

    val edgeInfos =
      for {
        edge @ Edge(v1, v2) ← edges
        vertexInfo1 ← vertexInfos1.vertexInfo(v1)
        vertexInfo2 ← vertexInfos2.vertexInfo(v2)
        start = vertexInfo1.outPorts(edge).down
        finish = vertexInfo2.inPorts(edge).up
      } yield EdgeInfo(v1, v2, start, finish, edge.reversed)

    val edgeRows = calculateEdgeOrdering(edgeInfos)

    val edgeZoneTopRow = if (vertexInfos1.isEmpty) -1 /* first layer */ else vertexInfos1.maxRow + 1
    def edgeBendRow(rowIndex: Int) = edgeZoneTopRow + rowIndex * 1 /* 2 */ + 1

    val edgeZoneBottomRow =
      if (edgeInfos.isEmpty)
        -1
      else if (edgeRows.isEmpty)
        edgeZoneTopRow + 2
      else
        edgeBendRow(edgeRows.values.max) + 2

    val edgeInfoToPoints: Map[EdgeInfo, List[Point]] =
      (for (edgeInfo @ EdgeInfo(startVertex, _, start, finish, _) ← edgeInfos) yield {
        val trueFinish = finish.translate(down = edgeZoneBottomRow + 1)
        val priorPoints: List[Point] = startVertex match {
          case dv: DummyVertex ⇒ incompleteEdges(dv)
          case _: RealVertex   ⇒ List(start)
        }
        val lastPriorPoint = priorPoints.last
        val points =
          if (lastPriorPoint.column == trueFinish.column) // No bend required
            priorPoints :+ trueFinish
          else {
            val row = edgeBendRow(edgeRows(edgeInfo))
            priorPoints ++ List(lastPriorPoint.copy(row = row), trueFinish.copy(row = row), trueFinish)
          }
        edgeInfo -> points
      }).toMap

    val edgeElements =
      for ((EdgeInfo(_, finishVertex: RealVertex, _, _, reversed), points) ← edgeInfoToPoints)
        yield EdgeDrawingElement(points, reversed, !reversed)

    val updatedIncompleteEdges: Map[DummyVertex, List[Point]] =
      for ((EdgeInfo(_, finishVertex: DummyVertex, _, _, _), points) ← edgeInfoToPoints)
        yield finishVertex -> points.init

    val updatedVertexInfos2 = vertexInfos2.down(edgeZoneBottomRow + 1)

    val vertexElements = updatedVertexInfos2.realVertexInfos.map {
      case (realVertex, info) ⇒
        val text = vertexRenderingStrategy.getText(realVertex.contents.asInstanceOf[V], info.contentRegion.dimension)
        VertexDrawingElement(info.region, text)
    }
    RowLayoutResult(vertexElements ++ edgeElements, updatedVertexInfos2, updatedIncompleteEdges)
  }

  private case class LayerVertexInfos(vertexInfos: Map[Vertex, VertexInfo]) {

    def vertexInfo(v: Vertex): Option[VertexInfo] = vertexInfos.get(v)

    def isEmpty = vertexInfos.isEmpty

    def maxRow = vertexInfos.values.map(_.region.bottomRow).max

    def maxColumn = vertexInfos.values.map(_.region.rightColumn).max

    def down(n: Int): LayerVertexInfos = copy(vertexInfos = Utils.transformValues(vertexInfos)(_.translate(down = n)))

    def realVertexInfos: List[(RealVertex, VertexInfo)] = vertexInfos.toList.collect {
      case (vertex: RealVertex, info) ⇒ (vertex, info)
    }

  }

  private def spaceVertices(layer: Layer, layerVertexInfos: LayerVertexInfos, diagramWidth: Int): LayerVertexInfos = {
    val excessSpace = diagramWidth - layerVertexInfos.maxColumn
    val spacing = math.max(excessSpace / (layerVertexInfos.vertexInfos.size + 1), 1)

    var pos = spacing
    val newVertexInfos =
      for (v ← layer.vertices) yield {
        val vertexInfo = layerVertexInfos.vertexInfo(v).get
        //      for ((v, vertexInfo) ← layerVertexInfos.vertexInfos) yield {
        val oldPos = pos
        pos += vertexInfo.region.width
        pos += spacing
        v -> vertexInfo.setLeft(oldPos)
      }
    LayerVertexInfos(newVertexInfos.toMap)
  }

  def layout(layering: Layering): Drawing = {

    var vertexInfosByLayer: Map[Layer, LayerVertexInfos] = Map()
    for ((previousLayerOpt, currentLayer, nextLayerOpt) ← Utils.withPreviousAndNext(layering.layers)) yield {
      val vertexInfos = calculateVertexInfo(currentLayer, layering.edges, previousLayerOpt, nextLayerOpt)
      vertexInfosByLayer += currentLayer -> vertexInfos
    }
    val diagramWidth = vertexInfosByLayer.values.map(_.vertexInfos.values.map(_.region.dimension.width + 1).sum).max

    vertexInfosByLayer = vertexInfosByLayer.map { case (layer, lvi) ⇒ layer -> spaceVertices(layer, lvi, diagramWidth) }

    var previousVertexInfos: LayerVertexInfos = LayerVertexInfos(Map())
    var incompleteEdges: Map[DummyVertex, List[Point]] = Map()
    var diagramElements: List[DrawingElement] = Nil
    for (layer ← layering.layers) {
      val vertexInfos = vertexInfosByLayer(layer)
      val RowLayoutResult(elements, updatedVertexInfos, updatedIncompletedEdges) =
        layoutRow(previousVertexInfos, vertexInfos, layering.edges, incompleteEdges)
      previousVertexInfos = updatedVertexInfos
      incompleteEdges = updatedIncompletedEdges
      diagramElements ++= elements
    }
    Drawing(diagramElements)
  }

}