package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.hadoop.fs.Path
import org.apache.spark.rdd._
import java.net.URI

import geotrellis.proj4.CRS

import scala.collection.JavaConverters._
import scala.reflect._

case class ZoomRange(minZoom: Int, maxZoom: Int)

object ZoomRange {
  implicit def ordering[A <: ZoomRange]: Ordering[A] = Ordering.by(_.maxZoom)
}

case class COGLayer[K, T <: CellGrid](
  layers: Map[ZoomRange, RDD[(K, GeoTiff[T])]], // Construct lower zoom levels off of higher zoom levels
  metadata: COGLayerMetadata
)

case class COGLayerMetadata(
  cellType: CellType,
  zoomRanges: Vector[ZoomRange],
  layouts: Map[Int, LayoutDefinition],
  extent: Extent,
  crs: CRS
) {
  /** Returns the ZoomRnage to read, and a Sequence of SpatialKey COGs to read, the total
    * GridBounds to read from that COG, and the sequence of GridBounds -> Keys that that
    * file should be cropped by
    */
  def getReadDefinitions[K: SpatialComponent: Boundable](keyBounds: KeyBounds[SpatialKey], zoom: Int): (ZoomRange, Seq[(SpatialKey, Int, GridBounds, Seq[(GridBounds, SpatialKey)])]) = {
    ???
  }

  /** Returns the ZoomRange and SpatialKey of the COG to be read for this key, index of overview, as well as the GridBounds to crop
    * that COG to */
  def getReadDefinition[K: SpatialComponent: Boundable](key: SpatialKey, zoom: Int): (ZoomRange, SpatialKey, Int, GridBounds) = {
    ???
  }
}

object COGLayer {

  case class ContextGeoTiff[K, T <: CellGrid](
    geoTiff: GeoTiff[T],
    metadata: TileLayerMetadata[K],
    zoom: Int,
    layoutScheme: ZoomedLayoutScheme,
    zoomRanges: Option[(Int, Int)],
    overviews: List[(Int, TileLayerMetadata[K])]
  )

  /**
    * Make it more generic? GeoTiffs are Iterables of (K, V)s // K - is a segment key, V - is a segment itself
    * Segments are in a row major order => profit?
    */
  def pyramidUp[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](itr: Iterable[(K, V)],
    endZoom: Int,
    layoutLevel: LayoutLevel,
    layoutScheme: LayoutScheme,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions
   )(implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): List[GeoTiff[V]] = {
    if(layoutLevel.zoom > 0) {
      val nextLayoutLevel@LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(layoutLevel)
      if (nextZoom >= endZoom) {
        val list: List[(K, V)] =
          itr
            .map { case (key, tile) =>
              val extent: Extent = key.getComponent[SpatialKey].extent(layoutLevel.layout)
              val newSpatialKey = nextLayout.mapTransform(extent.center)
              (key.setComponent(newSpatialKey), (key, tile))
            }
            .groupBy(_._1)
            .map { case (newKey, (nseq: Seq[(K, (K, V))])) =>
              val seq = nseq.map(_._2)
              val newExtent = newKey.getComponent[SpatialKey].extent(nextLayout)
              val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

              for ((oldKey, tile) <- seq) {
                val oldExtent = oldKey.getComponent[SpatialKey].extent(layoutLevel.layout)
                newTile.merge(newExtent, oldExtent, tile, NearestNeighbor)
              }

              //println(s"nexZoom: $nextZoom")
              //println(s"newKey: $newKey")
              //println(s"(newTile: V).asInstanceOf[Tile].dimensions: ${(newTile: V).asInstanceOf[Tile].dimensions}")

              (newKey, newTile: V)
            }.toList

        val gridBounds = list.gridBounds
        val keyBounds = {
          val GridBounds(colMin, rowMin, colMax, rowMax) = gridBounds
          val KeyBounds(minKey, maxKey) =
            md.bounds match {
              case kb: KeyBounds[K] => kb
              case EmptyBounds => throw new Exception("Empty RDD, can't generate a COG Layer.")
            }
          KeyBounds(
            minKey.setComponent[SpatialKey](SpatialKey(colMin, rowMin)),
            maxKey.setComponent[SpatialKey](SpatialKey(colMax, rowMax))
          )
        }

        val nextMd = TileLayerMetadata[K](
          cellType = md.cellType,
          layout = nextLayout,
          extent = md.extent,
          crs = md.crs,
          bounds = keyBounds
        )

        val ifdLayer: GeoTiff[V] =
          list.toGeoTiff(nextLayout, nextMd, options.copy(subfileType = Some(ReducedImage)))

        ifdLayer :: pyramidUp(list, endZoom, nextLayoutLevel, layoutScheme, md, options)
      } else Nil
    } else Nil
  }

  /**
    * Make it more generic? GeoTiffs are Iterables of (K, V)s // K - is a segment key, V - is a segment itself
    * Segments are in a row major order => profit?
    */
  def pyramidUpWithMetadata[
    K: SpatialComponent: Boundable: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](itr: Iterable[(K, V)],
    endZoom: Int,
    layoutLevel: LayoutLevel,
    layoutScheme: ZoomedLayoutScheme,
    md: TileLayerMetadata[K],
    options: GeoTiffOptions,
    metadataAccumulator: TileLayerMetadataMapAccumulator[K]
   )(implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): List[ContextGeoTiff[K, V]] = {
    if(layoutLevel.zoom > 0) {
      val nextLayoutLevel@LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(layoutLevel)
      if (nextZoom > endZoom) {
        val list: List[(K, V)] =
          itr
            .map { case (key, tile) =>
              val extent: Extent = key.getComponent[SpatialKey].extent(layoutLevel.layout)
              val newSpatialKey = nextLayout.mapTransform(extent.center)
              (key.setComponent(newSpatialKey), (key, tile))
            }
            .groupBy(_._1)
            .map { case (newKey, (nseq: Seq[(K, (K, V))])) =>
              val seq = nseq.map(_._2)
              val newExtent = newKey.getComponent[SpatialKey].extent(nextLayout)
              val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

              for ((oldKey, tile) <- seq) {
                val oldExtent = oldKey.getComponent[SpatialKey].extent(layoutLevel.layout)
                newTile.merge(newExtent, oldExtent, tile, NearestNeighbor)
              }

              //println(s"nexZoom: $nextZoom")
              //println(s"newKey: $newKey")
              //println(s"(newTile: V).asInstanceOf[Tile].dimensions: ${(newTile: V).asInstanceOf[Tile].dimensions}")

              (newKey, newTile: V)
            }.toList

        val gridBounds = list.gridBounds
        val keyBounds = {
          val GridBounds(colMin, rowMin, colMax, rowMax) = gridBounds
          val KeyBounds(minKey, maxKey) =
            md.bounds match {
              case kb: KeyBounds[K] => kb
              case EmptyBounds => throw new Exception("Empty RDD, can't generate a COG Layer.")
            }
          KeyBounds(
            minKey.setComponent[SpatialKey](SpatialKey(colMin, rowMin)),
            maxKey.setComponent[SpatialKey](SpatialKey(colMax, rowMax))
          )
        }

        val nextMd = TileLayerMetadata[K](
          cellType = md.cellType,
          layout = nextLayout,
          extent = md.extent,
          crs = md.crs,
          bounds = keyBounds
        )

        metadataAccumulator.add(nextZoom -> nextMd)

        val ifdLayer: GeoTiff[V] =
          list.toGeoTiff(nextLayout, nextMd, options.copy(subfileType = Some(ReducedImage)))

        val ifdContextLayer =
          ContextGeoTiff(ifdLayer, nextMd, nextZoom, layoutScheme, None, Nil)

        ifdContextLayer :: pyramidUpWithMetadata(list, endZoom, nextLayoutLevel, layoutScheme, nextMd, options, metadataAccumulator)
      } else Nil
    } else Nil
  }

  def apply[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme)
   (implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): RDD[(K, GeoTiff[V])] = {
    val md = rdd.metadata
    val sourceLayout = md.layout
    val options: GeoTiffOptions = GeoTiffOptions(storageMethod = Tiled(sourceLayout.tileCols, sourceLayout.tileRows))
    val LayoutLevel(_, endLayout) = layoutScheme.zoomOut(LayoutLevel(endZoom + 1, sourceLayout))

    val groupedByEndZoom =
      rdd
        .map { case (key, tile) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val endSpatialKey = endLayout.mapTransform(extent.center)

          //println(s"endZoom::${endZoom}::key.setComponent(endSpatialKey): ${key.setComponent(endSpatialKey)}")
          //println(s"key.setComponent(endSpatialKey): ${key.setComponent(endSpatialKey)}")

          (key.setComponent(endSpatialKey), (key, tile))
        }
        .groupByKey()

    val groupedPartitions = groupedByEndZoom.count().toInt

    val result: RDD[(K, GeoTiff[V])] = groupedByEndZoom
      .repartition(groupedPartitions)
      .mapPartitions { partition: Iterator[(K, (Iterable[(K, V)]))] =>
        // TODO: refactor, so ugly
        val list = partition.toList
        val flatList = list.flatMap(_._2)

        if(list.nonEmpty) {
          val sfc = list.head._1

          val gridBounds = flatList.gridBounds
          val keyBounds = {
            val GridBounds(colMin, rowMin, colMax, rowMax) = gridBounds
            val KeyBounds(minKey, maxKey) =
              md.bounds match {
                case kb: KeyBounds[K] => kb
                case EmptyBounds => throw new Exception("Empty RDD, can't generate a COG Layer.")
              }
            KeyBounds(
              minKey.setComponent[SpatialKey](SpatialKey(colMin, rowMin)),
              maxKey.setComponent[SpatialKey](SpatialKey(colMax, rowMax))
            )
          }

          val extent =
            sourceLayout
              .mapTransform(gridBounds)
              .intersection(md.extent)
              .getOrElse(md.extent)

          val currentMd = TileLayerMetadata[K](
            cellType = md.cellType,
            layout   = md.layout,
            extent   = extent,
            crs      = md.crs,
            bounds   = keyBounds
          )

          val overviews: List[GeoTiff[V]] =
            pyramidUp[K, V](
              flatList, endZoom,
              LayoutLevel(startZoom, sourceLayout),
              layoutScheme, currentMd,
              options.copy(subfileType = Some(ReducedImage))
            )

          val stitchedTile: GeoTiff[V] =
            flatList.toGeoTiff(
              sourceLayout, currentMd, options,
              overviews
            )

          Iterator(sfc -> stitchedTile)
        } else Iterator()
      }

    result
  }

  // rdd: RDD[(K, GeoTiff[V])] - tiff is the last overviews
  def applyCOG[
    K: SpatialComponent: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](rdd: RDD[(K, GeoTiff[V])], md: COGLayerMetadata, bounds: Bounds[K])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme)
   (implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): RDD[(K, GeoTiff[V])] = {
    println(s"startZoom: $startZoom")
    println(s"endZoom: $endZoom")

    // base - layout of segments
    // source - layout of a source tiff
    // end - layout of the target tiff
    val (sourceLayout, endLayout) =
      (md.layouts(startZoom + 1), md.layouts(endZoom))

    val options: GeoTiffOptions = GeoTiffOptions(storageMethod = Tiled(sourceLayout.tileCols, sourceLayout.tileRows))

    val groupedByEndZoom =
      rdd
        .map { case (key, tiff) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val endSpatialKey = endLayout.mapTransform(extent.center)

          (key.setComponent(endSpatialKey), (key, tiff))
        }
        .groupByKey()

    val groupedPartitions = groupedByEndZoom.count().toInt

    val result: RDD[(K, GeoTiff[V])] = groupedByEndZoom
      .repartition(groupedPartitions)
      .mapPartitions { partition: Iterator[(K, (Iterable[(K, GeoTiff[V])]))] =>
        // TODO: refactor, so ugly
        val list = partition.toList
        val flatList = list.flatMap(_._2)

        val flatListTile = flatList.map { case (k, v) => k -> v.tile }

        if(list.nonEmpty) {
          val sfc = list.head._1

          val gridBounds = flatListTile.gridBounds
          val currentExtent = flatList.map(_._2.extent).reduce(_ combine _)

          val keyBounds = {
            val GridBounds(colMin, rowMin, colMax, rowMax) = gridBounds
            val KeyBounds(minKey, maxKey) =
              bounds match {
                case kb: KeyBounds[K] => kb
                case EmptyBounds => throw new Exception("Empty RDD, can't generate a COG Layer.")
              }
            KeyBounds(
              minKey.setComponent[SpatialKey](SpatialKey(colMin, rowMin)),
              maxKey.setComponent[SpatialKey](SpatialKey(colMax, rowMax))
            )
          }

          val extent =
            sourceLayout
              .mapTransform(gridBounds)
              .intersection(currentExtent)
              .getOrElse(currentExtent)

          val currentMd = TileLayerMetadata[K](
            cellType = md.cellType,
            layout   = sourceLayout,
            extent   = extent,
            crs      = md.crs,
            bounds   = keyBounds
          )

          lazy val overviews: List[GeoTiff[V]] =
            pyramidUp[K, V](
              flatListTile, endZoom,
              LayoutLevel(startZoom, sourceLayout),
              layoutScheme, currentMd,
              options.copy(subfileType = Some(ReducedImage))
            )

          val stitchedTile: GeoTiff[V] =
            flatListTile.toGeoTiff(
              sourceLayout, currentMd, options,
              overviews
            )

          Iterator(sfc -> stitchedTile)
        } else Iterator()
      }

    result
  }

  def applyWithMetadata[
    K: SpatialComponent: Boundable: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: ZoomedLayoutScheme)
   (implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): RDD[(K, ContextGeoTiff[K, V])] = {
    val metadataAccumulator = new TileLayerMetadataMapAccumulator[K]
    rdd.sparkContext.register(metadataAccumulator, "metadata")

    val md = rdd.metadata
    val sourceLayout = md.layout
    val options: GeoTiffOptions = GeoTiffOptions(/*compression = Deflate, */ storageMethod = Tiled(sourceLayout.tileCols, sourceLayout.tileRows))
    val LayoutLevel(_, endLayout) = layoutScheme.zoomOut(LayoutLevel(endZoom + 1, sourceLayout))

    val groupedByEndZoom =
      rdd
        .map { case (key, tile) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val endSpatialKey = endLayout.mapTransform(extent.center)
          (key.setComponent(endSpatialKey), (key, tile))
        }
        .groupByKey()
        .cache()

    val groupedPartitions = groupedByEndZoom.count().toInt

    val res = groupedByEndZoom
      .repartition(groupedPartitions)
      .mapPartitions { partition: Iterator[(K, (Iterable[(K, V)]))] =>
        // TODO: refactor, so ugly
        val list = partition.toList
        val flatList = list.flatMap(_._2)

        if (list.nonEmpty) {
          val sfc = list.head._1

          val gridBounds = flatList.gridBounds
          val keyBounds = {
            val GridBounds(colMin, rowMin, colMax, rowMax) = gridBounds
            val KeyBounds(minKey, maxKey) =
              md.bounds match {
                case kb: KeyBounds[K] => kb
                case EmptyBounds => throw new Exception("Empty RDD, can't generate a COG Layer.")
              }
            KeyBounds(
              minKey.setComponent[SpatialKey](SpatialKey(colMin, rowMin)),
              maxKey.setComponent[SpatialKey](SpatialKey(colMax, rowMax))
            )
          }

          val extent =
            sourceLayout
              .mapTransform(gridBounds)
              .intersection(md.extent)
              .getOrElse(md.extent)

          val currentMd = TileLayerMetadata[K](
            cellType = md.cellType,
            layout   = md.layout,
            extent   = extent,
            crs      = md.crs,
            bounds   = keyBounds
          )

          metadataAccumulator.add(startZoom -> currentMd)

          val overviews: List[ContextGeoTiff[K, V]] =
            pyramidUpWithMetadata[K, V](
              flatList, endZoom,
              LayoutLevel(startZoom, sourceLayout),
              layoutScheme, currentMd,
              options.copy(subfileType = Some(ReducedImage)),
              metadataAccumulator
            )

          val stitchedTile: GeoTiff[V] =
            flatList.toGeoTiff(
              sourceLayout, currentMd, options,
              overviews.map(_.geoTiff)
            )

          val ifdContextLayer =
            ContextGeoTiff(
              stitchedTile, currentMd, startZoom, layoutScheme,
              Some(endZoom -> startZoom), overviews.map { o => o.zoom -> o.metadata }
            )

          Iterator(sfc -> ifdContextLayer)
        } else Iterator()
      }

    res.cache().foreach { _ => () } // force metadata computation

    val metadataMap = metadataAccumulator.value.asScala
    res.mapValues { value =>
      value.copy(
        metadata  = metadataMap.getOrElse(value.zoom, value.metadata),
        overviews = value.overviews.map { case (z, v) => z -> metadataMap.getOrElse(z, v) }
      )
    }
  }

  // maxTiffSize = 64mb
  // use accumulator to collect metadata for attribetu store during layer writes
  // instead of maxTiff size use cols rows
  def applyWithMetadataCalc[
    K: SpatialComponent: Boundable: Ordering: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])
   (startZoom: Int, layoutScheme: ZoomedLayoutScheme, maxTiffSize: Long = 8l * 1024 * 1024, minZoom: Option[Int] = None)
   (implicit tc: Iterable[(K, V)] => GeoTiffSegmentConstructMethods[K, V]): COGLayer[K, V] = {
    val endZoom = minZoom.getOrElse(0)
    val md = rdd.metadata

    val bandsCount = {
      val sample = rdd.take(1)(0)._2
      if(classTag[V].runtimeClass.isAssignableFrom(classTag[MultibandTile].runtimeClass))
        sample.asInstanceOf[MultibandTile].bandCount
      else 1
    }
    val bytesPerTile = md.cellType.bytes.toLong * md.tileLayout.tileSize * bandsCount

    val keyBounds =
      md.bounds match {
        case kb: KeyBounds[K] => kb
        case EmptyBounds => throw new Exception("Empty RDD, can't generate a COG Layer.")
      }

    val keyBoundsList: Seq[(Int, KeyBounds[K])] =
      (startZoom, keyBounds) :: transformKeyBounds(keyBounds, LayoutLevel(startZoom, md.layout), endZoom, layoutScheme)

    val bytesPerZoom: Seq[(Int, Long)] =
      keyBoundsList
        .map { case (zoom, kb) => zoom -> keyBoundsToRange(kb).length * bytesPerTile }
        .sortBy(- _._1)

    val chunks: List[ZoomRange] =
      bytesPerZoom
        .foldLeft(0l, List(List[Int]())) { case ((sliceBytes, zoomList @ zoomHead :: zoomTail), (zoom, bytes)) =>
          if (sliceBytes + bytes <= maxTiffSize) (sliceBytes + bytes, (zoom :: zoomHead) :: zoomTail)
          else (bytes, (zoom :: Nil) :: zoomList)
        }._2
        .flatMap {
          case Nil      => None
          case h :: Nil => Some(h -> h)
          case h :: t   => Some(h -> t.last)
        }
        .map { case (minZoom, maxZoom) => ZoomRange(minZoom, maxZoom) }

    val layouts =
      (endZoom to startZoom)
        .map { z => z -> layoutScheme.zoomOut(LayoutLevel(z + 1, md.layout)).layout }
        .toMap

    val cogMd =
      COGLayerMetadata(
        cellType   = md.cellType,
        zoomRanges = chunks.toVector,
        layouts    = layouts,
        extent     = md.extent,
        crs        = md.crs
      )

    val layers: Map[ZoomRange, RDD[(K, GeoTiff[V])]] =
      chunks
        .sorted
        .reverse
        .foldLeft(List[(ZoomRange, RDD[(K, GeoTiff[V])])]()) { case (acc, range) =>
          if(acc.isEmpty) {
            List(range -> apply(rdd)(range.maxZoom, range.minZoom, layoutScheme))
          } else {
            val previousLayer = acc.head._2.mapValues(_.overviews.last)

            val rzz = applyCOG(previousLayer, cogMd, bounds = md.bounds)(range.maxZoom, range.minZoom, layoutScheme)

            (range -> rzz) :: acc
          }
        }
        .toMap

    COGLayer[K, V](
      layers = layers, // Construct lower zoom levels off of higher zoom levels
      metadata = cogMd
    )
  }

  def write[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](cogs: RDD[(K, GeoTiff[V])])(keyIndex: KeyIndex[K], uri: URI): Unit = {
    val conf = HadoopConfiguration(cogs.sparkContext.hadoopConfiguration)
    cogs.foreach { case (key, tiff) =>
      println(s"$key: ${uri.toString}/${keyIndex.toIndex(key)}.tiff")
      HdfsUtils.write(new Path(s"${uri.toString}/${keyIndex.toIndex(key)}.tiff"), conf.get) { new GeoTiffWriter(tiff, _).write(true) }
    }
  }

  private def keyBoundsToRange[K: SpatialComponent](kb: KeyBounds[K]): Seq[K] = {
    val KeyBounds(minKey, maxKey) = kb
    val SpatialKey(minCol, minRow) = minKey.getComponent[SpatialKey]
    val SpatialKey(maxCol, maxRow) = maxKey.getComponent[SpatialKey]

    for {
      c <- minCol to maxCol
      r <- minRow to maxRow
    } yield minKey.setComponent[SpatialKey](SpatialKey(c, r))
  }

  private def nextKeyBounds[K: SpatialComponent](keyBounds: KeyBounds[K], layoutLevel: LayoutLevel, nextLayoutLevel: LayoutLevel): KeyBounds[K] = {
    val KeyBounds(minKey, maxKey) = keyBounds
    val LayoutLevel(_, layout) = layoutLevel
    val LayoutLevel(_, nextLayout) = nextLayoutLevel

    val extent = layout.extent
    val sourceRe = RasterExtent(extent, layout.layoutCols, layout.layoutRows)
    val targetRe = RasterExtent(extent, nextLayout.layoutCols, nextLayout.layoutRows)

    val minSpatialKey = minKey.getComponent[SpatialKey]
    val (minCol, minRow) = {
      val (x, y) = sourceRe.gridToMap(minSpatialKey.col, minSpatialKey.row)
      targetRe.mapToGrid(x, y)
    }

    val maxSpatialKey = maxKey.getComponent[SpatialKey]
    val (maxCol, maxRow) = {
      val (x, y) = sourceRe.gridToMap(maxSpatialKey.col, maxSpatialKey.row)
      targetRe.mapToGrid(x, y)
    }

    KeyBounds(
      minKey.setComponent(SpatialKey(minCol, minRow)),
      maxKey.setComponent(SpatialKey(maxCol, maxRow))
    )
  }

  private def transformKeyBounds[K: SpatialComponent](
    keyBounds: KeyBounds[K],
    layoutLevel: LayoutLevel,
    endZoom: Int,
    layoutScheme: LayoutScheme
  ): List[(Int, KeyBounds[K])] = {
    val KeyBounds(minKey, maxKey) = keyBounds
    val LayoutLevel(_, layout) = layoutLevel
    if (layoutLevel.zoom > 0) {
      val nextLayoutLevel@LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(layoutLevel)

      if (nextZoom < endZoom) Nil
      else {
        val extent = layout.extent
        val sourceRe = RasterExtent(extent, layout.layoutCols, layout.layoutRows)
        val targetRe = RasterExtent(extent, nextLayout.layoutCols, nextLayout.layoutRows)

        val minSpatialKey = minKey.getComponent[SpatialKey]
        val (minCol, minRow) = {
          val (x, y) = sourceRe.gridToMap(minSpatialKey.col, minSpatialKey.row)
          targetRe.mapToGrid(x, y)
        }

        val maxSpatialKey = maxKey.getComponent[SpatialKey]
        val (maxCol, maxRow) = {
          val (x, y) = sourceRe.gridToMap(maxSpatialKey.col, maxSpatialKey.row)
          targetRe.mapToGrid(x, y)
        }

        val kb =
          KeyBounds(
            minKey.setComponent(SpatialKey(minCol, minRow)),
            maxKey.setComponent(SpatialKey(maxCol, maxRow))
          )

        (nextZoom, kb) :: transformKeyBounds(kb, nextLayoutLevel, endZoom, layoutScheme)
      }
    } else Nil
  }
}
