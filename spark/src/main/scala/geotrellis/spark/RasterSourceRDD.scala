/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark

import geotrellis.spark.partition._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.layer._
import geotrellis.vector.Geometry
import geotrellis.util._
import org.apache.spark.rdd._
import org.apache.spark.{Partitioner, SparkContext}
import cats.syntax.option._

import scala.collection.mutable.ArrayBuilder
import scala.reflect.ClassTag
import java.time.ZonedDateTime

object RasterSourceRDD {
  final val DEFAULT_PARTITION_BYTES: Long = 128L * 1024 * 1024

  def read(
    readingSources: Seq[ReadingSource],
    layout: LayoutDefinition
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    read(readingSources, layout, DEFAULT_PARTITION_BYTES)

  def read(
    readingSources: Seq[ReadingSource],
    layout: LayoutDefinition,
    keyExtractor: KeyExtractor.Aux[SpaceTimeKey, ZonedDateTime]
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpaceTimeKey] =
    read(readingSources, layout, keyExtractor, DEFAULT_PARTITION_BYTES)

  def read(
    readingSources: Seq[ReadingSource],
    layout: LayoutDefinition,
    partitionBytes: Long
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    readPartitionBytes(readingSources, layout, KeyExtractor.spatialKeyExtractor, partitionBytes)

  def read(
    readingSources: Seq[ReadingSource],
    layout: LayoutDefinition,
    keyExtractor: KeyExtractor.Aux[SpaceTimeKey, ZonedDateTime],
    partitionBytes: Long
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpaceTimeKey] =
    readPartitionBytes(readingSources, layout, keyExtractor, partitionBytes)

  def readPartitionBytes[K: SpatialComponent: ClassTag, M: Boundable](
    readingSources: Seq[ReadingSource],
    layout: LayoutDefinition,
    keyExtractor: KeyExtractor.Aux[K, M],
    partitionBytes: Long
  )(implicit sc: SparkContext): MultibandTileLayerRDD[K] = {
    val summary = RasterSummary.fromSeq(readingSources.map(_.source), keyExtractor.getMetadata)
    val cellType = summary.cellType
    val tileSize = layout.tileCols * layout.tileRows * cellType.bytes
    val layerMetadata = summary.toTileLayerMetadata(layout, keyExtractor.getKey)

    def getNoDataTile = ArrayTile.alloc(cellType, layout.tileCols, layout.tileRows).fill(NODATA).interpretAs(cellType)

    val maxIndex = readingSources.map { _.sourceToTargetBand.values.max }.max
    val targetIndexes: Seq[Int] = 0 to maxIndex

    val sourcesRDD: RDD[(K, (Int, Option[MultibandTile]))] =
      sc.parallelize(readingSources, readingSources.size).flatMap { rs =>
        val m = keyExtractor.getMetadata(rs.source)
        val tileKeyTransform: SpatialKey => K = { sk => keyExtractor.getKey(m, sk) }
        val layoutSource = rs.source.tileToLayout(layout, tileKeyTransform)
        val keys = layoutSource.keys

        RasterSourceRDD.partition(keys, partitionBytes)( _ => tileSize).flatMap { _.flatMap { key =>
          rs.sourceToTargetBand.map { case (sourceBand, targetBand) =>
            (key, (targetBand, layoutSource.read(key, Seq(sourceBand))))
          }
        }.toTraversable }
      }

    sourcesRDD.persist()

    val repartitioned = {
      val count = sourcesRDD.count().toInt
      if (count > sourcesRDD.partitions.size) sourcesRDD.repartition(count)
      else sourcesRDD
    }

    val groupedSourcesRDD: RDD[(K, Iterable[(Int, Option[MultibandTile])])] =
      repartitioned.groupByKey()

    val result: RDD[(K, MultibandTile)] =
      groupedSourcesRDD.mapPartitions ({ partition =>
        val noDataTile = getNoDataTile

        partition.map { case (key, iter) =>
          val mappedBands: Map[Int, Option[MultibandTile]] = iter.toSeq.sortBy { _._1 }.toMap

          val tiles: Seq[Tile] =
            targetIndexes.map { index =>
              mappedBands.getOrElse(index, None) match {
                case Some(multibandTile) => multibandTile.band(0)
                case None => noDataTile
              }
            }

          key -> MultibandTile(tiles)
        }
      }, preservesPartitioning = true)

    sourcesRDD.unpersist()

    ContextRDD(result, layerMetadata)
  }

  def read(
    readingSourcesRDD: RDD[ReadingSource],
    layout: LayoutDefinition,
    partitioner: Option[Partitioner] = None
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    read(readingSourcesRDD, layout, KeyExtractor.spatialKeyExtractor, partitioner)

  def read[K: SpatialComponent: ClassTag, M: Boundable](
    readingSourcesRDD: RDD[ReadingSource],
    layout: LayoutDefinition,
    keyExtractor: KeyExtractor.Aux[K, M],
    partitioner: Option[Partitioner]
  )(implicit sc: SparkContext): MultibandTileLayerRDD[K] = {
    val rasterSourcesRDD = readingSourcesRDD.map { _.source }
    val summary = RasterSummary.fromRDD(rasterSourcesRDD, keyExtractor.getMetadata)
    val cellType = summary.cellType
    val layerMetadata = summary.toTileLayerMetadata(layout, keyExtractor.getKey)

    def getNoDataTile = ArrayTile.alloc(cellType, layout.tileCols, layout.tileRows).fill(NODATA).interpretAs(cellType)

    val maxIndex = readingSourcesRDD.map { _.sourceToTargetBand.values.max }.reduce { _ max _ }
    val targetIndexes: Seq[Int] = 0 to maxIndex

    val keyedRDD: RDD[(K, (Int, Option[MultibandTile]))] =
      readingSourcesRDD.mapPartitions ({ partition =>
        partition.flatMap { rs =>
          val m = keyExtractor.getMetadata(rs.source)
          val tileKeyTransform: SpatialKey => K = { sk => keyExtractor.getKey(m, sk) }
          val layoutSource = rs.source.tileToLayout(layout, tileKeyTransform)

          layoutSource.keys.flatMap { key =>
            rs.sourceToTargetBand.map { case (sourceBand, targetBand) =>
              (key, (targetBand, layoutSource.read(key, Seq(sourceBand))))
            }
          }
        }
      })

    val groupedRDD: RDD[(K, Iterable[(Int, Option[MultibandTile])])] = {
      // The number of partitions estimated by RasterSummary can sometimes be much
      // lower than what the user set. Therefore, we assume that the larger value
      // is the optimal number of partitions to use.
      val partitionCount =
      math.max(keyedRDD.getNumPartitions, summary.estimatePartitionsNumber)

      keyedRDD.groupByKey(partitioner.getOrElse(SpatialPartitioner[K](partitionCount)))
    }

    val result: RDD[(K, MultibandTile)] =
      groupedRDD.mapPartitions ({ partition =>
        val noDataTile = getNoDataTile

        partition.map { case (key, iter) =>
          val mappedBands: Map[Int, Option[MultibandTile]] = iter.toSeq.sortBy { _._1 }.toMap

          val tiles: Seq[Tile] =
            targetIndexes.map { index =>
              mappedBands.getOrElse(index, None) match {
                case Some(multibandTile) => multibandTile.band(0)
                case None => noDataTile
              }
            }

          key -> MultibandTile(tiles)
        }
      }, preservesPartitioning = true)

    ContextRDD(result, layerMetadata)
  }

  def tiledLayerRDD(
    sources: RDD[RasterSource],
    layout: LayoutDefinition
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    tiledLayerRDD(sources, layout, KeyExtractor.spatialKeyExtractor, None, NearestNeighbor, None, None)

  def tiledLayerRDD(
    sources: RDD[RasterSource],
    layout: LayoutDefinition,
    geometry: Geometry
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    tiledLayerRDD(sources, layout, KeyExtractor.spatialKeyExtractor, geometry.some, NearestNeighbor, None, None)

  def tiledLayerRDD(
    sources: RDD[RasterSource],
    layout: LayoutDefinition,
    resampleMethod: ResampleMethod
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    tiledLayerRDD(sources, layout, KeyExtractor.spatialKeyExtractor, None, resampleMethod, None, None)

  def tiledLayerRDD(
    sources: RDD[RasterSource],
    layout: LayoutDefinition,
    resampleMethod: ResampleMethod,
    geometry: Geometry
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    tiledLayerRDD(sources, layout, KeyExtractor.spatialKeyExtractor, geometry.some, resampleMethod, None, None)

  /**
   * On tiling more than a single MultibandTile may get into a group that correspond to the same key.
   * By default the tiledLayerRDD function flattens all bands and converts every group into a single MultibandTile.
   * To override this behavior it is possible to set the partitionTransform function, i.e.:
   * {{{
   * partitionTransform = {
   *   case iter if iter.nonEmpty => iter.map(_.tile).reduce(_ merge _)
   *   case _                     => MultibandTile(Nil)
   * }
   * }}}
   * */
  def tiledLayerRDD[K: SpatialComponent: Boundable: ClassTag, M: Boundable](
    sources: RDD[RasterSource],
    layout: LayoutDefinition,
    keyExtractor: KeyExtractor.Aux[K, M],
    geometry: Option[Geometry] = None,
    resampleMethod: ResampleMethod = NearestNeighbor,
    rasterSummary: Option[RasterSummary[M]] = None,
    partitioner: Option[Partitioner] = None,
    partitionTransform: Iterable[Raster[MultibandTile]] => MultibandTile = { iter => MultibandTile(iter.flatMap(_.tile.bands)) }
  )(implicit sc: SparkContext): MultibandTileLayerRDD[K] = {
    val summary = rasterSummary.getOrElse(RasterSummary.fromRDD(sources, keyExtractor.getMetadata))
    val layerMetadata = summary.toTileLayerMetadata(layout, keyExtractor.getKey)

    val tiledLayoutSourceRDD =
      sources.map { rs =>
        val m = keyExtractor.getMetadata(rs)
        val tileKeyTransform: SpatialKey => K = { sk => keyExtractor.getKey(m, sk) }
        rs.tileToLayout(layout, tileKeyTransform, resampleMethod)
      }

    val rasterRegionRDD: RDD[(K, RasterRegion)] =
      tiledLayoutSourceRDD.flatMap { source =>
        val keyedRasterRegions = source.keyedRasterRegions()
        geometry.fold(keyedRasterRegions) { geom => keyedRasterRegions.filter {
          case (key, _) => layerMetadata.keyToExtent(key).intersects(geom)
        } }
      }

    // The number of partitions estimated by RasterSummary can sometimes be much
    // lower than what the user set. Therefore, we assume that the larger value
    // is the optimal number of partitions to use.
    val partitionCount =
      math.max(rasterRegionRDD.getNumPartitions, summary.estimatePartitionsNumber)

    val tiledRDD: RDD[(K, MultibandTile)] =
      rasterRegionRDD
        .groupByKey(partitioner.getOrElse(SpatialPartitioner[K](partitionCount)))
        .mapValues { iter => partitionTransform(iter.flatMap(_.raster.toSeq)) }

    ContextRDD(tiledRDD, layerMetadata)
  }

  def spatial(
    sources: Seq[RasterSource],
    layout: LayoutDefinition,
    strategy: OverviewStrategy = OverviewStrategy.DEFAULT,
    partitionBytes: Long = DEFAULT_PARTITION_BYTES
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    apply(sources, layout, KeyExtractor.spatialKeyExtractor, partitionBytes, strategy)

  def spatial(source: RasterSource, layout: LayoutDefinition)(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    spatial(Seq(source), layout)

  def temporal(
    sources: Seq[RasterSource],
    layout: LayoutDefinition,
    keyExtractor: KeyExtractor.Aux[SpaceTimeKey, ZonedDateTime],
    strategy: OverviewStrategy = OverviewStrategy.DEFAULT,
    partitionBytes: Long = DEFAULT_PARTITION_BYTES
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpaceTimeKey] =
    apply(sources, layout, keyExtractor, partitionBytes, strategy)

  def temporal(source: RasterSource, layout: LayoutDefinition, keyExtractor: KeyExtractor.Aux[SpaceTimeKey, ZonedDateTime])(implicit sc: SparkContext): MultibandTileLayerRDD[SpaceTimeKey] =
    temporal(Seq(source), layout, keyExtractor)

  def apply[K: SpatialComponent: Boundable: ClassTag, M: Boundable](
    sources: Seq[RasterSource],
    layout: LayoutDefinition,
    keyExtractor: KeyExtractor.Aux[K, M],
    partitionBytes: Long = DEFAULT_PARTITION_BYTES,
    strategy: OverviewStrategy = OverviewStrategy.DEFAULT
  )(implicit sc: SparkContext): MultibandTileLayerRDD[K] = {
    val summary = RasterSummary.fromSeq(sources, keyExtractor.getMetadata)
    val extent = summary.extent
    val cellType = summary.cellType
    val tileSize = layout.tileCols * layout.tileRows * cellType.bytes
    val layerMetadata = summary.toTileLayerMetadata(layout, keyExtractor.getKey)

    val sourcesRDD: RDD[(RasterSource, Array[SpatialKey])] =
      sc.parallelize(sources).flatMap { source =>
        val keys: Traversable[SpatialKey] =
          extent.intersection(source.extent) match {
            case Some(intersection) => layout.mapTransform.keysForGeometry(intersection.toPolygon())
            case None => Seq.empty[SpatialKey]
          }
        partition(keys, partitionBytes)( _ => tileSize).map { res => (source, res) }
      }

    sourcesRDD.persist()

    val repartitioned = {
      val count = sourcesRDD.count().toInt
      if (count > sourcesRDD.partitions.size) sourcesRDD.repartition(count)
      else sourcesRDD
    }

    val result: RDD[(K, MultibandTile)] =
      repartitioned.flatMap { case (source, keys) =>
        val m = keyExtractor.getMetadata(source)
        val tileKeyTransform: SpatialKey => K = { sk => keyExtractor.getKey(m, sk) }
        val tileSource = source.tileToLayout(layout, tileKeyTransform, NearestNeighbor, strategy)
        tileSource.readAll(keys.map(tileKeyTransform).iterator)
      }

    sourcesRDD.unpersist()

    ContextRDD(result, layerMetadata)
  }

  /** Partition a set of chunks not to exceed certain size per partition */
  private def partition[T: ClassTag](
    chunks: Traversable[T],
    maxPartitionSize: Long
  )(chunkSize: T => Long = { c: T => 1L }): Array[Array[T]] = {
    if (chunks.isEmpty) {
      Array[Array[T]]()
    } else {
      val partition = ArrayBuilder.make[T]
      partition.sizeHintBounded(128, chunks)
      var partitionSize: Long = 0L
      var partitionCount: Long = 0L
      val partitions = ArrayBuilder.make[Array[T]]

      def finalizePartition(): Unit = {
        val res = partition.result()
        if (res.nonEmpty) partitions += res
        partition.clear()
        partitionSize = 0L
        partitionCount = 0L
      }

      def addToPartition(chunk: T): Unit = {
        partition += chunk
        partitionSize += chunkSize(chunk)
        partitionCount += 1
      }

      for (chunk <- chunks) {
        if ((partitionCount == 0) || (partitionSize + chunkSize(chunk)) < maxPartitionSize)
          addToPartition(chunk)
        else {
          finalizePartition()
          addToPartition(chunk)
        }
      }

      finalizePartition()
      partitions.result()
    }
  }
}
