package geotrellis.spark.graphx

import geotrellis.spark._
import geotrellis.spark.graphx._

import geotrellis.raster._

import org.scalatest.FunSpec

class RasterToGraphSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Raster to Graph Spec") {

    ifCanRunSpark {

      val nd = NODATA

      it("should create a correct graph for the given rdd") {
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

        graphRDD.numVertices should be (9 * 4)

        graphRDD.numEdges should be (107)

        val groupedVertices = graphRDD.vertices
          .groupBy(_._2._1)
          .map { case(key, iter) => (key, iter.map(_._1).toSeq) }
          .collect

        val base = Seq(0, 1, 2, 3, 4, 5)
        var coordinatesSet = Set((0, 0), (0, 1), (0, 2), (1, 0), (1, 1), (1, 2))
        for ((key, vertices) <- groupedVertices) {
          val SpatialKey(col, row) = key
          val correctVertices = base.map(_ + (row * 18 + col * 6))

          vertices
            .sortWith(_ < _)
            .zip(correctVertices)
            .foreach { case(v1, v2) =>
              v1 should be (v2)
          }

          coordinatesSet = coordinatesSet - ((row, col))
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
              v1._2 should be(v2)
            }
        }

        graphRDD.metaData should be (rasterRDD.metaData)
      }

    }

  }
}
