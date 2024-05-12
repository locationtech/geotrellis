/*
 * Copyright 2018 Azavea
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

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector.io.json.CrsFormats
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.util._
import geotrellis.layer._
import geotrellis.spark.tiling._
import geotrellis.store.json.{Implicits => StoreCodecs}

import org.apache.spark.{Partitioner, SparkContext}
import org.apache.spark.rdd._

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits
    extends geotrellis.spark.buffer.Implicits
    with CrsFormats
    with StoreCodecs
    with geotrellis.spark.clip.Implicits
    with geotrellis.spark.costdistance.Implicits
    with geotrellis.spark.crop.Implicits
    with geotrellis.spark.density.Implicits
    with geotrellis.spark.distance.Implicits
    with geotrellis.spark.equalization.Implicits
    with geotrellis.spark.filter.Implicits
    with geotrellis.spark.join.Implicits
    with geotrellis.spark.knn.Implicits
    with geotrellis.spark.mapalgebra.focal.hillshade.Implicits
    with geotrellis.spark.mapalgebra.focal.Implicits
    with geotrellis.spark.mapalgebra.Implicits
    with geotrellis.spark.mapalgebra.local.Implicits
    with geotrellis.spark.mapalgebra.local.temporal.Implicits
    with geotrellis.spark.mapalgebra.zonal.Implicits
    with geotrellis.spark.mask.Implicits
    with geotrellis.spark.matching.Implicits
    with geotrellis.spark.merge.Implicits
    with geotrellis.spark.partition.Implicits
    with geotrellis.spark.regrid.Implicits
    with geotrellis.spark.reproject.Implicits
    with geotrellis.spark.resample.Implicits
    with geotrellis.spark.rasterize.Implicits
    with geotrellis.spark.sigmoidal.Implicits
    with geotrellis.spark.split.Implicits
    with geotrellis.spark.stitch.Implicits
    with geotrellis.spark.summary.Implicits
    with geotrellis.spark.tiling.Implicits
    with geotrellis.spark.timeseries.Implicits
    with geotrellis.spark.viewshed.Implicits
    with Serializable {

  /** Auto wrap a partitioner when something is requestion an Option[Partitioner];
    * useful for Options that take an Option[Partitioner]
    */
  implicit def partitionerToOption(partitioner: Partitioner): Option[Partitioner] =
    Some(partitioner)

  implicit class WithContextWrapper[K, V, M](val rdd: RDD[(K, V)] with Metadata[M]) {
    def withContext[K2, V2](f: RDD[(K, V)] => RDD[(K2, V2)]) =
      new ContextRDD(f(rdd), rdd.metadata)

    def mapContext[M2](f: M => M2) =
      new ContextRDD(rdd, f(rdd.metadata))
  }

  implicit def tupleToRDDWithMetadata[K, V, M](tup: (RDD[(K, V)], M)): RDD[(K, V)] with Metadata[M] =
    ContextRDD(tup._1, tup._2)

  implicit class withContextRDDMethods[
    K: ClassTag: SpatialComponent,
    V <: CellGrid[Int]: ClassTag,
    M: GetComponent[*, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M]) extends ContextRDDMethods[K, V, M](rdd) {

    def toRasters: RDD[(K, Raster[V])] = {
      val mt = rdd.metadata.getComponent[LayoutDefinition].mapTransform

      rdd.mapPartitions({ partition =>
        partition.map { case (k, v) =>
          (k, Raster(v, mt(k.getComponent[SpatialKey])))
        }
      }, preservesPartitioning = true)
    }
  }

  implicit class withTileLayerRDDMethods[K: SpatialComponent: ClassTag](val self: TileLayerRDD[K])
    extends TileLayerRDDMethods[K] {
    def toGeoTiffs(
      tags: Tags = Tags.empty,
      options: GeoTiffOptions = GeoTiffOptions.DEFAULT
    ): RDD[(K, SinglebandGeoTiff)] = {
      val mt = self.metadata.layout.mapTransform
      val crs = self.metadata.crs

      self.mapPartitions({ partition =>
        partition.map { case (k, v) =>
          (k, SinglebandGeoTiff(v, mt(k.getComponent[SpatialKey]), crs, tags, options))
        }
      }, preservesPartitioning = true)
    }
  }

  implicit class withMultibandTileLayerRDDMethods[K: SpatialComponent: ClassTag](val self: MultibandTileLayerRDD[K])
    extends MultibandTileLayerRDDMethods[K] {

    def toGeoTiffs(
      tags: Tags = Tags.empty,
      options: GeoTiffOptions = GeoTiffOptions.DEFAULT
    ): RDD[(K, MultibandGeoTiff)] = {
      val mt = self.metadata.layout.mapTransform
      val crs = self.metadata.crs

      self.mapPartitions({ partition =>
        partition.map { case (k, v) =>
          (k, MultibandGeoTiff(v, mt(k.getComponent[SpatialKey]), crs, tags, options))
        }
      }, preservesPartitioning = true)
    }
  }

  implicit class withCellGridLayoutRDDMethods[K: SpatialComponent: ClassTag, V <: CellGrid[Int], M: GetComponent[*, LayoutDefinition]](val self: RDD[(K, V)] with Metadata[M])
      extends CellGridLayoutRDDMethods[K, V, M]

  implicit class withProjectedExtentRDDMethods[K: Component[*, ProjectedExtent], V <: CellGrid[Int]](val rdd: RDD[(K, V)]) {
    def toRasters: RDD[(K, Raster[V])] =
      rdd.mapPartitions({ partition =>
        partition.map { case (key, value) =>
          (key, Raster(value, key.getComponent[ProjectedExtent].extent))
        }
      }, preservesPartitioning = true)
  }

  implicit class withTileProjectedExtentRDDMethods[K: Component[*, ProjectedExtent]: Component[*, CRS]](val rdd: RDD[(K, Tile)]) {
    def toGeoTiffs(
      tags: Tags = Tags.empty,
      options: GeoTiffOptions = GeoTiffOptions.DEFAULT
    ): RDD[(K, SinglebandGeoTiff)] =
      rdd.mapPartitions({ partition =>
        partition.map { case (key, value) =>
          (key, SinglebandGeoTiff(value, key.getComponent[ProjectedExtent].extent, key.getComponent[CRS], tags, options))
        }
      }, preservesPartitioning = true)
  }

  implicit class withMultibandTileProjectedExtentRDDMethods[K: Component[*, ProjectedExtent]: Component[*, CRS]](val rdd: RDD[(K, MultibandTile)]) {
    def toGeoTiffs(
      tags: Tags = Tags.empty,
      options: GeoTiffOptions = GeoTiffOptions.DEFAULT
    ): RDD[(K, MultibandGeoTiff)] =
      rdd.mapPartitions({ partition =>
        partition.map { case (key, value) =>
          (key, MultibandGeoTiff(value, key.getComponent[ProjectedExtent].extent, key.getComponent[CRS], tags, options))
        }
      }, preservesPartitioning = true)
  }

  implicit class withSpatialContextRDDMethods[V: ClassTag](val self: RDD[(SpatialKey, V)] with Metadata[TileLayerMetadata[SpatialKey]])
    extends ContextRDDMethods[SpatialKey, V, TileLayerMetadata[SpatialKey]](self) {

    def keyToProjectedExtent: RDD[(ProjectedExtent, V)] = {
      val mt = self.metadata.layout.mapTransform
      val crs = self.metadata.crs

      self.mapPartitions { partition =>
        partition.map { case (k, v) => (ProjectedExtent(mt(k), crs), v) }
      }
    }
  }

  implicit class withTemporalContextRDDMethods[V: ClassTag](val self: RDD[(SpaceTimeKey, V)] with Metadata[TileLayerMetadata[SpaceTimeKey]])
    extends ContextRDDMethods[SpaceTimeKey, V, TileLayerMetadata[SpaceTimeKey]](self) {

    def keyToTemporalProjectedExtent: RDD[(TemporalProjectedExtent, V)] = {
      val mt = self.metadata.layout.mapTransform
      val crs = self.metadata.crs

      self.mapPartitions { partition =>
        partition.map { case (k, v) => (TemporalProjectedExtent(mt(k), crs, k.instant), v) }
      }
    }
  }

  implicit class withCollectionConversionMethods[K, V, M](val rdd: RDD[(K, V)] with Metadata[M]) {
    def toCollection: Seq[(K, V)] with Metadata[M] = ContextCollection(rdd.collect(), rdd.metadata)
  }

  implicit class withRddConversionMethods[K, V, M](val seq: Seq[(K, V)] with Metadata[M]) {
    def toRDD(implicit sc: SparkContext): RDD[(K, V)] with Metadata[M] = ContextRDD(sc.parallelize(seq), seq.metadata)
  }

  implicit class withProjectedExtentTemporalTilerKeyMethods[K: Component[*, ProjectedExtent]: Component[*, TemporalKey]](val self: K) extends TilerKeyMethods[K, SpaceTimeKey] {
    def extent = self.getComponent[ProjectedExtent].extent
    def translate(spatialKey: SpatialKey): SpaceTimeKey = SpaceTimeKey(spatialKey, self.getComponent[TemporalKey])
  }

  implicit class withProjectedExtentTilerKeyMethods[K: Component[*, ProjectedExtent]](val self: K) extends TilerKeyMethods[K, SpatialKey] {
    def extent = self.getComponent[ProjectedExtent].extent
    def translate(spatialKey: SpatialKey) = spatialKey
  }

  implicit class withCollectMetadataMethods[K1, V <: CellGrid[Int]](rdd: RDD[(K1, V)]) extends Serializable {
    /** The `Int` is the zoom level if ingested with the produced Metadata. */
    def collectMetadata[K2: Boundable: SpatialComponent](crs: CRS, layoutScheme: LayoutScheme)
        (implicit ev: K1 => TilerKeyMethods[K1, K2]): (Int, TileLayerMetadata[K2]) = {
      CollectTileLayerMetadata.fromRDD[K1, V, K2](rdd, crs, layoutScheme)
    }

    def collectMetadata[K2: Boundable: SpatialComponent](crs: CRS, layout: LayoutDefinition)
        (implicit ev: K1 => TilerKeyMethods[K1, K2]): TileLayerMetadata[K2] = {
      CollectTileLayerMetadata.fromRDD[K1, V, K2](rdd, crs, layout)
    }

    /** The `Int` is the zoom level if ingested with the produced Metadata. */
    def collectMetadata[K2: Boundable: SpatialComponent](layoutScheme: LayoutScheme)
        (implicit ev: K1 => TilerKeyMethods[K1, K2], ev1: GetComponent[K1, ProjectedExtent]): (Int, TileLayerMetadata[K2]) = {
      CollectTileLayerMetadata.fromRDD[K1, V, K2](rdd, layoutScheme)
    }

    /** The `Int` is the zoom level if ingested with the produced Metadata. */
    def collectMetadata[K2: Boundable: SpatialComponent](crs: CRS, size: Int, zoom: Int)
        (implicit ev: K1 => TilerKeyMethods[K1, K2], ev1: GetComponent[K1, ProjectedExtent]): (Int, TileLayerMetadata[K2]) = {
      CollectTileLayerMetadata.fromRDD[K1, V, K2](rdd, ZoomedLayoutScheme(crs, size), zoom)
    }

    def collectMetadata[K2: Boundable: SpatialComponent](layout: LayoutDefinition)
        (implicit ev: K1 => TilerKeyMethods[K1, K2], ev1: GetComponent[K1, ProjectedExtent]): TileLayerMetadata[K2] = {
      CollectTileLayerMetadata.fromRDD[K1, V, K2](rdd, layout)
    }
  }

  implicit class withGetBoundsMethod[K: Boundable, V <: CellGrid[Int]](rdd: RDD[(K, V)]) extends Serializable {
    def getBounds: Bounds[K] =
      rdd
        .map { case (k, tile) => Bounds(k, k) }
        .fold(EmptyBounds) { _ combine  _ }
  }
}
