package geotrellis.spark.op.global

import geotrellis.spark._

import geotrellis.raster.Tile

import spire.syntax.cfor._

import org.apache.spark.graphx._
import org.apache.spark.graphx.lib._

import collection.mutable.ArrayBuffer

import reflect.ClassTag

object CostDistance {

  private type DummyType = Byte

  private val DummyValue = 0.toByte

  private val Sqrt2 = math.sqrt(2)

  private def constructVertices[K](offset: Long, area: Int) = {
    val vertices = Array.ofDim[(VertexId, DummyType)](area)

    cfor(0)(_ < area, _ + 1) {i => vertices(i) = (offset + 1, DummyValue) }

    vertices
  }

  private def getCost(tile: Tile)(start: (Int, Int))(end: (Int, Int)) = {
    val (sc, sr) = start
    val (ec, er) = end
    val r = tile.get(sc, sr) + tile.get(ec, er)

    if (math.abs(sc - ec) == 1 && math.abs(sr - er) == 1) (r / Sqrt2).toInt
    else r / 2
  }

  private def constructEdges(offset: Long, tile: Tile) = {
    val (cols, rows) = tile.dimensions

    val cost = getCost(tile)(_)
    val edges = new ArrayBuffer[Edge[Int]](cols * rows * 4)
    var vertexId = offset
    cfor(0)(_ < rows - 1, _ + 1) { i =>
      cfor(0)(_ < cols, _ + 1) { j =>
        val c = cost(j, i)
        if (j != rows - 1) {
          edges += Edge(vertexId, vertexId + 1, c(j + 1, i))
          edges += Edge(vertexId, vertexId + 1 + cols, c(j + 1, i + 1))
        }

        if (j != 0)
          edges += Edge(vertexId, vertexId - 1 + cols, c(j - 1, i + 1))

        edges += Edge(vertexId, vertexId + cols, c(j, i + 1))

        vertexId += 1
      }
    }

    edges.toArray
  }

  def apply[K](rasterRDD: RasterRDD[K], points: Seq[(Int, Int)])
    (implicit keyClassTag: ClassTag[K]): RasterRDD[K] = {
    val metaData = rasterRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val (layoutCols, layoutRows) = (gridBounds.width - 1, gridBounds.height - 1)
    val (cols, rows) = tileLayout.tileDimensions
    val area = cols * rows

    def getOffset(key: K) = {
      val SpatialKey(col, row) = key
      getOffsetByColAndRow(col, row)
    }

    def getOffsetByColAndRow(col: Long, row: Long) =
      area * layoutCols * row + area * col

    val verticesRDD = rasterRDD.map { case(key, tile) => // O(n)
      val offset = getOffset(key)
      constructVertices(offset, area)
    }.flatMap(x => x)

    val edgesRDD = rasterRDD.map { case(key, tile) => // O(n)
      val offset = getOffset(key)
      constructEdges(offset, tile)
    }.flatMap(x => x)

    val graph = Graph(verticesRDD, edgesRDD)

    val sources = points.map { case (c, r) => getOffsetByColAndRow(c, r) }

    val resGraph = ShortestPaths.run(graph, sources)

    //val shortestPaths = resGraph.vertices.map(a: Map[])

    ???
  }

}
