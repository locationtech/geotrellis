package geotrellis.raster

import geotrellis.vector._
import geotrellis.raster.interpolation._

import spire.syntax.cfor._

package object mosaic {
// =======
// import geotrellis.raster._
// import geotrellis.raster.reproject._
// import geotrellis.proj4.CRS
// import org.apache.spark.rdd._
// import monocle.syntax._
// import geotrellis.spark._
// import spire.syntax.cfor._

// package object ingest {
//   type Tiler[T, K] = (RDD[(T, Tile)], RasterMetaData) => RasterRDD[K]
//   type IngestKey[T] = KeyComponent[T, ProjectedExtent]

//   implicit class IngestKeyWrapper[T: IngestKey](key: T) {
//     val _projectedExtent = implicitly[IngestKey[T]]

//     def projectedExtent: ProjectedExtent = key &|-> _projectedExtent.lens get

//     def updateProjectedExtent(pe: ProjectedExtent): T =
//       key &|-> _projectedExtent.lens set(pe)
//   }

//   // TODO: Move this to geotrellis.vector
//   case class ProjectedExtent(extent: Extent, crs: CRS)

//   object ProjectedExtent {
//     implicit object ProjectedExtentComponent extends IdentityComponent[ProjectedExtent]
//   }

//   implicit def projectedExtentToSpatialKeyTiler: Tiler[ProjectedExtent, SpatialKey] = {
//       val getExtent = (inKey: ProjectedExtent) => inKey.extent
//       val createKey = (inKey: ProjectedExtent, spatialComponent: SpatialKey) => spatialComponent
//       Tiler(getExtent, createKey)
//     }

//   implicit class ReprojectWrapper[T: IngestKey](rdd: RDD[(T, Tile)]) {
//     def reproject(destCRS: CRS): RDD[(T, Tile)] = Reproject(rdd, destCRS)
//   }

// >>>>>>> upstream/master:spark/src/main/scala/geotrellis/spark/ingest/package.scala
  /** Tile methods used by the mosaicing function to merge tiles. */
  implicit class TileMerger(val tile: MutableArrayTile) {
    def merge(other: Tile): MutableArrayTile = {
      Seq(tile, other).assertEqualDimensions
      if(tile.cellType.isFloatingPoint) {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.getDouble(col, row))) {
              tile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      } else {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.get(col, row))) {
              tile.setDouble(col, row, other.get(col, row))
            }
          }
        }
      }

      tile
    }

    def merge(extent: Extent, otherExtent: Extent, other: Tile): MutableArrayTile =
      merge(extent, otherExtent, other, NearestNeighbor)

    def merge(extent: Extent, otherExtent: Extent, other: Tile, method: InterpolationMethod): MutableArrayTile =
      otherExtent & extent match {
        case PolygonResult(sharedExtent) =>
          val re = RasterExtent(extent, tile.cols, tile.rows)
          val GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
          val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

          if(tile.cellType.isFloatingPoint) {
            val interpolate = Interpolation(method, other, otherExtent).interpolateDouble _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.getDouble(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  tile.setDouble(col, row, interpolate(x, y))
                }
              }
            }
          } else {
            val interpolate = Interpolation(method, other, otherExtent).interpolate _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.get(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  tile.set(col, row, interpolate(x, y))
                }
              }
            }

          }

          tile
        case _ =>
          tile
      }
  }
}
