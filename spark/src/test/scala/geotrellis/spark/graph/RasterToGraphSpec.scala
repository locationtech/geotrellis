package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.raster._

import collection.mutable.ArrayBuffer

import org.apache.spark.graphx._

import org.scalatest.FunSpec

import spire.syntax.cfor._

class RasterToGraphSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Raster to Graph Spec") {

    val Eps = 1e-9

    ifCanRunSpark {

      def testGraph(rasterRDD: RasterRDD[SpatialKey], graphRDD: GraphRDD[SpatialKey]) = {
        val tile = rasterRDD.stitch

        graphRDD.numVertices should be (tile.size)

        val (cols, rows) = tile.dimensions
        val edges = ((rows - 1) * cols + (cols - 1) *
          rows + (cols - 1) * (rows - 1) * 2) * 2

        graphRDD.numEdges should be (edges)

        val metaData = rasterRDD.metaData
        val tileLayout = metaData.tileLayout
        val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)

        val gridBounds = metaData.gridBounds
        val (layoutCols, layoutRows) = (gridBounds.width - 1, gridBounds.height - 1)

        val groupedVertices = graphRDD.vertices
          .groupBy(_._2._1)
          .map { case(key, iter) => (key, iter.map(_._1).toSeq) }
          .collect

        var coordinatesSet = (for (c <- 0 until layoutCols;
          r <- 0 until layoutRows) yield ((r, c))).toSet

        for ((key, vertices) <- groupedVertices) {
          val SpatialKey(layoutCol, layoutRow) = key
          val buff = ArrayBuffer[Long]()
          cfor(0)(_ < tileRows, _ + 1) { tileRow =>
            cfor(0)(_ < tileCols, _ + 1) { tileCol =>
              buff += ((layoutRow * tileRows + tileRow)
                * tileCols * layoutCols + layoutCol * tileCols + tileCol)
            }
          }

          val correctVertices = buff.toArray

          vertices
            .sortWith(_ < _)
            .zip(correctVertices)
            .foreach { case(v1, v2) =>
              v1 should be (v2)
          }

          coordinatesSet = coordinatesSet - ((layoutRow, layoutCol))
        }

        coordinatesSet.size should be (0)

        val vertices = graphRDD.vertices.collect
        val tiles = rasterRDD.collect

        vertices
          .groupBy { case(id, (key, value)) => key }
          .map { case(key, iter) =>
            (key, iter.map { case(id, (key, value)) => (id, value) } )
        }.foreach { case (key, iter) =>
            val array = tiles.filter(_._1 == key).head._2.toArray
            iter.sortWith(_._1 < _._1).zip(array).foreach { case(v1, v2) =>
              if (v1._2.isNaN && v2 != NODATA) fail
              else if (!v1._2.isNaN && v2 == NODATA) fail
              else if (!v1._2.isNaN && v2 != NODATA) v1._2 should be(v2)
            }
        }

        graphRDD.metaData should be (rasterRDD.metaData)
      }

      def testEdges(graphRDD: GraphRDD[SpatialKey], tests: Seq[(Long, Long, Double)]) = {
        val edges = graphRDD.edges.collect
        tests.foreach { case(from, to, v) =>
          edges.filter(e => e.srcId == from && e.dstId == to).headOption match {
            case Some(e) => {
              if (!e.attr.isNaN && !v.isNaN) e.attr should be (v +- Eps)
              else if (e.attr.isNaN && !v.isNaN) fail
              else if (!e.attr.isNaN && v.isNaN) fail
            }
            case None => withClue(s"from: $from, to: $to wasn't found") {
              fail
            }
          }
        }
      }

      val nd = NODATA

      it("should create a correct graph for the given rdd #1") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd,7, 1,   1, 1, 1,   1, 1, 1,
            9, 1, 1,   2, 2, 2,   1, 3, 1,

            3, 8, 1,   3, 3, 3,   1, 1, 2,
            2, 1, 7,   1, nd,1,   8, 1, 1
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        val graphRDD = rasterRDD.toGraph

        testGraph(rasterRDD, graphRDD)

        val edges = Seq[(Long, Long, Double)](
          (0, 1, Double.NaN),
          (11, 21, 4 / math.sqrt(2)),
          (11, 19, 9 / math.sqrt(2)),
          (10, 19, 9 / 2.0),
          (10, 20, math.sqrt(2))
        )

        testEdges(graphRDD, edges)
      }

      it("should create a correct graph for the given rdd #2") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            1, 3, 4,  4, 3, 2,
            4, 6, 2,  3, 7, 6,
            5, 8, 7,  5, 6, 6,

            1, 4, 5,  nd, 5, 1,
            4, 7, 5,  nd, 2, 6,
            1, 2, 2,  1, 3, 4
          ), 6, 6),
          TileLayout(2, 2, 3, 3)
        )

        val graphRDD = rasterRDD.toGraph

        testGraph(rasterRDD, graphRDD)

        val edges = Seq(
          (0L, 1L, 2.0),
          (15L, 20L, 10 / math.sqrt(2)),
          (14L, 21L, Double.NaN),
          (21L, 28L, Double.NaN)
        )

        testEdges(graphRDD, edges)
      }

    }

  }
}
