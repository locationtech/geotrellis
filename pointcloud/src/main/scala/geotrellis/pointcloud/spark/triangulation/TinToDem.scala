package geotrellis.pointcloud.spark.triangulation

import geotrellis.raster._
import geotrellis.raster.triangulation.DelaunayRasterizer
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.vector.triangulation._

import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.rdd.RDD
import spire.syntax.cfor._

object TinToDem {
  case class Options(
    cellType: CellType = DoubleConstantNoDataCellType,
    boundsBuffer: Option[Double] = None
  )

  object Options {
    def DEFAULT = Options()
  }

  def apply(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, extent: Extent, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] =
    rdd
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.flatMap { case (key, neighbors) =>
          val extent @ Extent(xmin, ymin, xmax, ymax) =
            layoutDefinition.mapTransform(key)

          var len = 0
          neighbors.foreach { case (_, (_, arr)) =>
            len += arr.length
          }
          val points = Array.ofDim[Coordinate](len)
          var j = 0
          options.boundsBuffer match {
            case Some(b) =>
              val bufferedExtent =
                Extent(
                  xmin - b,
                  ymin - b,
                  xmax + b,
                  ymax + b
                )

              neighbors.foreach { case (_, (_, tilePoints)) =>
                cfor(0)(_ < tilePoints.length, _ + 1) { i =>
                  val p = tilePoints(i)
                  // Only consider points within `boundsBuffer` of the extent
                  if(bufferedExtent.contains(p.x, p.y)) {
                    points(j) = p
                    j += 1
                  }
                }
              }
            case None =>
              neighbors.foreach { case (_, (_, tilePoints)) =>
                cfor(0)(_ < tilePoints.length, _ + 1) { i =>
                  points(j) = tilePoints(i)
                  j += 1
                }
              }
          }

          if(j > 2) {
            val pointSet =
              DelaunayPointSet(points)

            val delaunay = DelaunayTriangulation(pointSet)

            val re =
              RasterExtent(
                extent,
                layoutDefinition.tileCols,
                layoutDefinition.tileRows
              )

            val tile =
              ArrayTile.empty(options.cellType, re.cols, re.rows)

            DelaunayRasterizer.rasterizeDelaunayTriangulation(delaunay, re, tile)

            Some((key, tile))
          } else {
            None
          }
        }
      }, preservesPartitioning = true)

  def withStitch(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, extent: Extent, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] = {

    // Assumes that a partitioner has already been set

    val triangulations: RDD[(SpatialKey, DelaunayTriangulation)] =
      rdd
        .mapValues { points =>
          DelaunayTriangulation(points)
        }

    val borders: RDD[(SpatialKey, BoundaryDelaunay)] =
      triangulations
        .mapPartitions{ iter =>
          iter.map{ case (sk, dt) => {
            val ex: Extent = layoutDefinition.mapTransform(sk)
            (sk, BoundaryDelaunay(dt, ex))
          }
          }}

    borders
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.map { case (key, neighbors) =>
          val newNeighbors =
            neighbors.map { case (direction, (key2, border)) =>
              val ex = layoutDefinition.mapTransform(key2)
              (direction, (border, ex))
            }
          (key, newNeighbors.toMap)
        }
      }, preservesPartitioning = true)
      .join(triangulations)
      .mapPartitions({ partition =>
        partition.map { case (key, (borders, triangulation)) => // : (Map[Direction, (BoundaryDelaunay, Extent)], DelaunayTriangulation)
          val stitched = StitchedDelaunay(borders)

          val extent = layoutDefinition.mapTransform(key)
          val re =
            RasterExtent(
              extent,
              layoutDefinition.tileCols,
              layoutDefinition.tileRows
            )

          val tile = stitched.rasterize(re, options.cellType)(triangulation)

          (key, tile)
        }
      }, preservesPartitioning = true)
  }

  def allStitch(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, extent: Extent, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] = {

    // Assumes that a partitioner has already been set

    val triangulations: RDD[(SpatialKey, DelaunayTriangulation)] =
      rdd
        .mapValues { points =>
          DelaunayTriangulation(points)
        }

    triangulations
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.map { case (key, neighbors) =>
          val newNeighbors =
            neighbors.map { case (direction, (key2, dt)) =>
              val ex = layoutDefinition.mapTransform(key2)
              (direction, (dt, ex))
            }
          (key, newNeighbors.toMap)
        }
      }, preservesPartitioning = true)
      .join(triangulations)
      .mapPartitions({ partition =>
        partition.map { case (key, (nbhd, triangulation)) => // : (Map[Direction, (BoundaryDelaunay, Extent)], DelaunayTriangulation)
          val stitched = StitchedDelaunay(nbhd, false)

          val extent = layoutDefinition.mapTransform(key)
          val re =
            RasterExtent(
              extent,
              layoutDefinition.tileCols,
              layoutDefinition.tileRows
            )

          val tile = stitched.rasterize(re, options.cellType)(triangulation)

          (key, tile)
        }
      }, preservesPartitioning = true)
  }
}
