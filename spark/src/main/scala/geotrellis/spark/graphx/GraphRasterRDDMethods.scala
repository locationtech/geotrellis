package geotrellis.spark.graphx

import geotrellis.spark._
import geotrellis.spark.op.focal._

import geotrellis.raster._

import org.apache.spark.graphx._

import spire.syntax.cfor._

import collection.mutable.ArrayBuffer

object GraphRasterRDDMethods {

  private val Sqrt2 = math.sqrt(2)

  private def getCost(tile: Tile)(
    start: (Int, Int))(
    end: (Int, Int))(
    diagonal: Boolean): Double =
      getCost(tile, tile)(start)(end)(diagonal)

  private def getCost(t1: Tile, t2: Tile)(
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

  private def constructEdges(offset: Long, tile: Tile) = {
    val (cols, rows) = tile.dimensions

    val cost = getCost(tile)(_)
    val edges = new ArrayBuffer[Edge[Double]](cols * rows * 5)
    var vertexId = offset
    cfor(0)(_ < rows, _ + 1) { i =>
      cfor(0)(_ < cols, _ + 1) { j =>
        val c = cost(j, i)
        val isRight = j == cols - 1
        val isLeft = j == 0
        val isBottom = i == rows - 1

        if (!isRight)
          edges += Edge(vertexId, vertexId + 1, c(j + 1, i)(false))

        if (!isRight && !isBottom)
          edges += Edge(vertexId, vertexId + 1 + cols, c(j + 1, i + 1)(true))

        if (!isLeft && !isBottom)
          edges += Edge(vertexId, vertexId - 1 + cols, c(j - 1, i + 1)(true))

        if (!isBottom)
          edges += Edge(vertexId, vertexId + cols, c(j, i + 1)(false))

        vertexId += 1
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
    val (cols, rows) = (tileLayout.tileCols, tileLayout.tileRows)
    val area = cols * rows

    def getOffset[K](key: K) = {
      val SpatialKey(col, row) = key
      getOffsetByColAndRow(col, row)
    }

    def getOffsetByColAndRow(col: Long, row: Long) =
      area * layoutCols * row + area * col

    val verticesRDD = rasterRDD.flatMap { case(key, tile) =>
      val offset = getOffset(key)
      val vertices = Array.ofDim[(VertexId, (K, Double))](area)
      cfor(0)(_ < area, _ + 1) { i =>
        val v = tile.get(i % cols, i / cols)
        vertices(i) = (offset + i, (key, if (v == NODATA) Double.NaN else v))
      }

      vertices
    }

    implicit val _scImplicit = _sc
    val edgesRDD = rasterRDD.zipWithNeighbors.flatMap { case(key, tile, neighbors) =>
      val SpatialKey(col, row) = key
      val upperLeftOffset = getOffsetByColAndRow(col, row)

      val edges = constructEdges(upperLeftOffset, tile)

      val lowerLeftOffset = upperLeftOffset + (rows - 1) * cols

      neighbors.sw match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col - 1, row + 1) + cols - 1
          edges += Edge(
            lowerLeftOffset,
            otherOffset,
            getCost(tile, t)(0, rows - 1)(cols - 1, 0)(true)
          )
        }
        case _ =>
      }

      val lowerRightOffset = upperLeftOffset + area - 1

      neighbors.se match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col + 1, row + 1)
          edges += Edge(
            lowerRightOffset,
            otherOffset,
            getCost(tile, t)(cols - 1, rows - 1)(0, 0)(true)
          )
        }
        case _ =>
      }

      val upperRightOffset = upperLeftOffset + cols - 1

      neighbors.e match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col + 1, row)
          val cost = getCost(tile, t)(_)
          cfor(0)(_ < rows, _ + 1) { i =>
            val cOff = upperRightOffset + i * cols
            val c = cost(cols - 1, i)(_)
            if (i != 0)
              edges += Edge(cOff, otherOffset + (i - 1) * cols, c(0, i - 1)(true))

            if (i != rows - 1)
              edges += Edge(cOff, otherOffset + (i + 1) * cols, c(0, i + 1)(true))

            edges += Edge(cOff, otherOffset + i * cols, c(0, i)(false))
          }
        }
        case _ =>
      }

      neighbors.s match {
        case Some(t) => {
          val otherOffset = getOffsetByColAndRow(col, row + 1)
          val cost = getCost(tile, t)(_)
          cfor(0)(_ < cols, _ + 1) { i =>
            val cOff = lowerLeftOffset + i
            val c = cost(i, rows - 1)(_)
            if (i != 0)
              edges += Edge(cOff, otherOffset + i - 1, c(i - 1, 0)(true))

            if (i != cols - 1)
              edges += Edge(cOff, otherOffset + i + 1, c(i + 1, 0)(true))

            edges += Edge(cOff, otherOffset + i, c(i, 0)(false))
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
