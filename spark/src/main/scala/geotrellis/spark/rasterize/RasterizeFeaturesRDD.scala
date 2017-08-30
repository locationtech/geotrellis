package geotrellis.spark.rasterize

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._

import org.apache.spark.rdd._
import org.apache.spark.{HashPartitioner, Partitioner}
import org.apache.spark.rdd._

import scala.collection.immutable.VectorBuilder

import spire.syntax.cfor._


object RasterizeFeaturesRDD {

  def fromFeature[G <: Geometry](
    features: RDD[Feature[G, Double]],
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val features2 = features.map({ feature =>
      Feature[G, CellValue](feature.geom, CellValue(feature.data, 0))
    })
    fromFeaturePriority(features2, cellType, layout, options, partitioner)
  }

  /**
   * Rasterize an RDD of Geometry objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.  Value
   * will be converted to type matching specified [[CellType]].  The
   * word "Priority" in the function name is being used as an
   * adjective, not as a noun.
   *
   * @param features Cell values for cells intersecting a feature consisting of Feature(geometry,value)
   * @param layout Raster layer layout for the result of rasterization
   * @param cellType [[CellType]] for creating raster tiles
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def fromFeaturePriority[G <: Geometry](
    features: RDD[Feature[G, CellValue]],
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None,
    usePriority: Boolean = false
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val layoutRasterExtent = RasterExtent(layout.extent, layout.layoutCols, layout.layoutRows)
    val layoutRasterizerOptions = Rasterizer.Options(includePartial=true, sampleType=PixelIsArea)
    val fudge = math.min(layoutRasterExtent.cellwidth, layoutRasterExtent.cellheight) * 0.01

    /**
      * "Priority" is being used as an adjective, not as a noun.
      */
    def mergePriority(
      leftTile: MutableArrayTile,
      leftPriority: ShortArrayTile,
      rightTile: MutableArrayTile,
      rightPriority: ShortArrayTile
    ): (MutableArrayTile, ShortArrayTile) = {
      Seq(leftTile, rightTile, leftPriority, rightPriority).assertEqualDimensions()

      leftTile.cellType match {
        case BitCellType =>
          cfor(0)(_ < leftTile.rows, _ + 1) { row =>
            cfor(0)(_ < leftTile.cols, _ + 1) { col =>
              val leftv = leftTile.get(col, row)
              val rightv = rightTile.get(col, row)
              val rightp = rightPriority.get(col, row)
              if (leftv == 0 && rightv == 1) { // merge seems to treat 0 as nodata
                leftTile.set(col, row, rightv)
                leftPriority.setDouble(col, row, rightp)
              }
            }
          }
        case ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
          // Assume 0 as the transparent value
          cfor(0)(_ < leftTile.rows, _ + 1) { row =>
            cfor(0)(_ < leftTile.cols, _ + 1) { col =>
              val leftv = leftTile.get(col, row)
              val leftp = leftPriority.get(col, row)
              val rightv = rightTile.get(col, row)
              val rightp = rightPriority.get(col, row)
              if ((leftv == 0 && rightv != 0) || (leftv != 0 && rightv != 0 && leftp < rightp)) {
                leftTile.set(col, row, rightTile.get(col, row))
                leftPriority.setDouble(col, row, rightp)
              }
            }
          }
        case FloatCellType | DoubleCellType =>
          // Assume 0.0 as the transparent value
          cfor(0)(_ < leftTile.rows, _ + 1) { row =>
            cfor(0)(_ < leftTile.cols, _ + 1) { col =>
              val leftv = leftTile.getDouble(col, row)
              val leftp = leftPriority.get(col, row)
              val rightv = rightTile.getDouble(col, row)
              val rightp = rightPriority.get(col, row)
              if ((leftv == 0.0 && rightv != 0.0) || (leftv != 0.0 && rightv != 0.0 && leftp < rightp)) {
                leftTile.setDouble(col, row, rightv)
                leftPriority.setDouble(col, row, rightp)
              }
            }
          }
        case x if x.isFloatingPoint =>
          cfor(0)(_ < leftTile.rows, _ + 1) { row =>
            cfor(0)(_ < leftTile.cols, _ + 1) { col =>
              val leftv = leftTile.getDouble(col, row)
              val leftnd = isNoData(leftTile.getDouble(col, row))
              val leftp = leftPriority.get(col, row)
              val rightv = rightTile.getDouble(col, row)
              val rightnd = isNoData(rightTile.getDouble(col, row))
              val rightp = rightPriority.get(col, row)
              if ((leftnd && !rightnd) || (!leftnd && !rightnd && leftp < rightp)) {
                leftTile.setDouble(col, row, rightv)
                leftPriority.setDouble(col, row, rightp)
              }
            }
          }
        case _ =>
          cfor(0)(_ < leftTile.rows, _ + 1) { row =>
            cfor(0)(_ < leftTile.cols, _ + 1) { col =>
              val leftv = leftTile.get(col, row)
              val leftnd = isNoData(leftTile.get(col, row))
              val leftp = leftPriority.get(col, row)
              val rightv = rightTile.get(col, row)
              val rightnd = isNoData(rightTile.get(col, row))
              val rightp = rightPriority.get(col, row)
              if ((leftnd && !rightnd) || (!leftnd && !rightnd && leftp < rightp)) {
                leftTile.set(col, row, rightv)
                leftPriority.setDouble(col, row, rightp)
              }
            }
          }
      }

      (leftTile, leftPriority)
    }

    def lineToPolygons(line: Line): Seq[Polygon] = {
      line.points
        .sliding(2)
        .map({ case Array(a: Point, b: Point) =>
          Polygon(
            a, b,
            Point(b.x+fudge, b.y+fudge),
            Point(a.x+fudge, a.y+fudge),
            a
          ) })
        .toList
    }

    def multiLineToPolygons(mline: MultiLine): Seq[Polygon] = {
      mline.lines.flatMap({ line => lineToPolygons(line) })
    }

    /** Key geometry by spatial keys of intersecting tiles */
    def keyGeom(
      feature: Feature[Geometry, CellValue]
    ): Iterator[(SpatialKey, (Feature[Geometry, CellValue], SpatialKey))] = {
      val geoms = feature.geom match {
        case l: Line => lineToPolygons(l)
        case ml: MultiLine => multiLineToPolygons(ml)
        case g => List(g)
      }
      var keySet = Set.empty[SpatialKey]

      geoms.foreach({geom =>
        Rasterizer.foreachCellByGeometry(
          geom,
          layoutRasterExtent,
          layoutRasterizerOptions
        )({ (col: Int, row: Int) =>
          keySet = keySet + SpatialKey(col, row)
        })
      })

      keySet.toIterator.map { key => (key, (feature, key)) }
    }

    // key the geometry to intersecting tiles so it can be rasterized in the map-side combine
    val keyed: RDD[(SpatialKey, (Feature[Geometry, CellValue], SpatialKey))] =
      features.flatMap { feature => keyGeom(feature) }

    val createTile = (tup: (Feature[Geometry, CellValue], SpatialKey)) => {
      val (feature, key) = tup
      val tile = ArrayTile.empty(cellType, layout.tileCols, layout.tileRows)
      val ztile =
        if (usePriority)
          ShortArrayTile.empty(
            layout.tileCols,
            layout.tileRows,
            ShortConstantNoDataCellType)
        else
          null
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)

      feature.geom.foreach(re, options)({ (x: Int, y: Int) =>
        val priority = tup._1.data.zindex
        tile.setDouble(x, y, feature.data.value)
        if (usePriority)
          ztile.set(x, y, priority)
      })

      (tile, ztile): (MutableArrayTile, ShortArrayTile)
    }

    val updateTile = (
      pair: (MutableArrayTile, ShortArrayTile),
      tup: (Feature[Geometry, CellValue], SpatialKey)
    ) => {
      val (feature, key) = tup
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)
      val (tile, ztile) = pair

      if (usePriority) {
        val priority = tup._1.data.zindex
        feature.geom.foreach(re, options)({ (x: Int, y: Int) =>
          if (isNoData(pair._1.getDouble(x, y)) || (pair._2.get(x, y) < priority)) {
            tile.setDouble(x, y, feature.data.value)
            ztile.set(x, y, priority)
          }
        })
      } else {
        feature.geom.foreach(re, options)({
          tile.setDouble(_, _, feature.data.value)
        })
      }

      (tile, ztile): (MutableArrayTile, ShortArrayTile)
    }

    val mergeTiles = (pair1: (MutableArrayTile, ShortArrayTile), pair2: (MutableArrayTile, ShortArrayTile)) => {
      val (left, leftPriority) = pair1
      val (right, rightPriority) = pair2

      if (usePriority) {
        mergePriority(left, leftPriority, right, rightPriority)
        (left, leftPriority): (MutableArrayTile, ShortArrayTile)
      } else {
        (pair1._1.merge(pair2._1).mutable, pair1._2): (MutableArrayTile, ShortArrayTile)
      }
    }

    val tiles: RDD[(SpatialKey, MutableArrayTile)] =
      keyed.combineByKeyWithClassTag[(MutableArrayTile, ShortArrayTile)](
        createCombiner = createTile,
        mergeValue = updateTile,
        mergeCombiners = mergeTiles,
        partitioner.getOrElse(new HashPartitioner(features.getNumPartitions))
      )
        .map({ (tup: (SpatialKey, (MutableArrayTile, ShortArrayTile))) => (tup._1, tup._2._1) })

    ContextRDD(tiles.asInstanceOf[RDD[(SpatialKey, Tile)]], layout)
  }
}
