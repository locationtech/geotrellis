package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.spark.op.focal._

import geotrellis.raster._

import org.apache.spark.graphx._

import spire.syntax.cfor._

import collection.mutable.ArrayBuffer

object GraphRasterRDDMethods {

  private val Sqrt2 = math.sqrt(2)

  @inline
  private final def getCost(tile: Tile)(
    start: (Int, Int))(
    end: (Int, Int))(
    diagonal: Boolean): Double =
    getCost(tile, tile)(start)(end)(diagonal)

  @inline
  private final def getCost(t1: Tile, t2: Tile)(
    start: (Int, Int))(
    end: (Int, Int))(
    diagonal: Boolean): Double = {
    val (sc, sr) = start
    val (ec, er) = end
    val v1 = t1.get(sc, sr)
    val v2 = t2.get(ec, er)

    if (v1 == NODATA || v2 == NODATA) Double.NaN
    else {
      val r = v1 + v2

      if (diagonal) r / Sqrt2
      else r / 2.0
    }
  }

  @inline
  private final def constructEdges(tile: Tile, getVertexId: (Int, Int) => Long) = {
    val (cols, rows) = tile.dimensions

    val cost = getCost(tile)(_)
    val edges = new ArrayBuffer[Edge[Double]](cols * rows * 5)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val vertexId = getVertexId(col, row)

        val c = cost(col, row)(_)

        val isRight = col == cols - 1
        val isLeft = col == 0
        val isBottom = row == rows - 1

        if (!isRight) {
          val otherVertexId = getVertexId(col + 1, row)
          val edgeCost = c(col + 1, row)(false)

          edges += Edge(vertexId, otherVertexId, edgeCost)
          edges += Edge(otherVertexId, vertexId, edgeCost)
        }

        if (!isRight && !isBottom) {
          val otherVertexId = getVertexId(col + 1, row + 1)
          val edgeCost = c(col + 1, row + 1)(true)

          edges += Edge(vertexId, otherVertexId, edgeCost)
          edges += Edge(otherVertexId, vertexId, edgeCost)
        }

        if (!isLeft && !isBottom) {
          val otherVertexId = getVertexId(col - 1, row + 1)
          val edgeCost = c(col - 1, row + 1)(true)

          edges += Edge(vertexId, otherVertexId, edgeCost)
          edges += Edge(otherVertexId, vertexId, edgeCost)
        }

        if (!isBottom) {
          val otherVertexId = getVertexId(col, row + 1)
          val edgeCost = c(col, row + 1)(false)

          edges += Edge(vertexId, otherVertexId, edgeCost)
          edges += Edge(otherVertexId, vertexId, edgeCost)
        }
      }
    }

    edges
  }

}

trait GraphRasterRDDMethods[K] extends RasterRDDMethods[K] {

  import GraphRasterRDDMethods._

  val _sc: SpatialComponent[K]

  def toGraph: GraphRDD[K] = {
    val metaData = rasterRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val (layoutCols, layoutRows) = (gridBounds.width - 1, gridBounds.height - 1)
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)

    def getVertexId(layoutCol: Long, layoutRow: Long)
      (tileCol: Int, tileRow: Int) = ((layoutRow * tileRows + tileRow)
        * tileCols * layoutCols + layoutCol * tileCols + tileCol)

    val verticesRDD = rasterRDD.flatMap { case(key, tile) =>
      val SpatialKey(layoutCol, layoutRow) = key
      val getVertexIdForTile = getVertexId(layoutCol, layoutRow) _

      val vertices = Array.ofDim[(VertexId, (K, Double))](tile.size)

      cfor(0)(_ < tile.size, _ + 1) { i =>
        val (tileCol, tileRow) = (i % tileCols, i / tileCols)
        val v = tile.get(tileCol, tileRow)
        val vertexId = getVertexIdForTile(tileCol, tileRow)
        vertices(i) = (vertexId, (key, if (v == NODATA) Double.NaN else v))
      }

      vertices
    }

    implicit val _scImplicit = _sc
    val edgesRDD = rasterRDD.zipWithNeighbors.flatMap { case(key, tile, neighbors) =>
      val SpatialKey(layoutCol, layoutRow) = key
      val getVertexIdForTile = getVertexId(layoutCol, layoutRow) _

      val edges = constructEdges(tile, getVertexIdForTile)

      neighbors.sw match {
        case Some(otherTile) => {
          val vertexId = getVertexIdForTile(0, tileRows - 1)
          val otherVertexId = getVertexId(layoutCol - 1, layoutRow + 1)(tileCols - 1, 0)

          val cost = getCost(tile, otherTile)(0, tileRows - 1)(tileCols - 1, 0)(true)

          edges += Edge(vertexId, otherVertexId, cost)
          edges += Edge(otherVertexId, vertexId, cost)
        }
        case _ =>
      }

      neighbors.se match {
        case Some(otherTile) => {
          val vertexId = getVertexIdForTile(tileCols - 1, tileRows - 1)
          val otherVertexId = getVertexId(layoutCol + 1, layoutRow + 1)(0, 0)

          val cost = getCost(tile, otherTile)(tileCols - 1, tileRows - 1)(0, 0)(true)

          edges += Edge(vertexId, otherVertexId, cost)
          edges += Edge(otherVertexId, vertexId, cost)
        }
        case _ =>
      }

      neighbors.e match {
        case Some(otherTile) => {
          val cost = getCost(tile, otherTile) _
          val getVertexIdForOtherTile = getVertexId(layoutCol + 1, layoutRow) _

          cfor(0)(_ < tileRows, _ + 1) { row =>
            val vertexId = getVertexIdForTile(tileCols - 1, row)

            val c = cost(tileCols - 1, row)(_)

            val isTop = row == 0
            val isBottom = row == tileRows - 1

            if (!isTop) {
              val otherVertexId = getVertexIdForOtherTile(0, row - 1)
              val edgeCost = c(0, row - 1)(true)

              edges += Edge(vertexId, otherVertexId, edgeCost)
              edges += Edge(otherVertexId, vertexId, edgeCost)
            }

            if (!isBottom) {
              val otherVertexId = getVertexIdForOtherTile(0, row + 1)
              val edgeCost = c(0, row + 1)(true)

              edges += Edge(vertexId, otherVertexId, edgeCost)
              edges += Edge(otherVertexId, vertexId, edgeCost)
            }

            val otherVertexId = getVertexIdForOtherTile(0, row)
            val edgeCost = c(0, row)(false)

            edges += Edge(vertexId, otherVertexId, edgeCost)
            edges += Edge(otherVertexId, vertexId, edgeCost)
          }
        }
        case _ =>
      }

      neighbors.s match {
        case Some(otherTile) => {
          val cost = getCost(tile, otherTile)(_)
          val getVertexIdForOtherTile = getVertexId(layoutCol, layoutRow + 1) _

          cfor(0)(_ < tileCols, _ + 1) { col =>
            val vertexId = getVertexIdForTile(col, tileRows - 1)

            val c = cost(col, tileRows - 1)(_)

            val isLeft = col == 0
            val isRight = col == tileCols - 1

            if (!isLeft) {
              val otherVertexId = getVertexIdForOtherTile(col - 1, 0)
              val edgeCost = c(col - 1, 0)(true)

              edges += Edge(vertexId, otherVertexId, edgeCost)
              edges += Edge(otherVertexId, vertexId, edgeCost)
            }

            if (!isRight) {
              val otherVertexId = getVertexIdForOtherTile(col + 1, 0)
              val edgeCost = c(col + 1, 0)(true)

              edges += Edge(vertexId, otherVertexId, edgeCost)
              edges += Edge(otherVertexId, vertexId, edgeCost)
            }

            val otherVertexId = getVertexIdForOtherTile(col, 0)
            val edgeCost = c(col, 0)(false)

            edges += Edge(vertexId, otherVertexId, edgeCost)
            edges += Edge(otherVertexId, vertexId, edgeCost)
          }
        }
        case _ =>
      }

      edges
    }

    val graph = Graph(verticesRDD, edgesRDD)
    new GraphRDD(graph, rasterRDD.metaData)
  }

}
