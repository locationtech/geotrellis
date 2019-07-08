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

package geotrellis.spark.store.hadoop.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layer.SpatialComponent
import geotrellis.store.LayerId
import geotrellis.store.Reader
import geotrellis.store.hadoop._
import geotrellis.store.hadoop.cog._
import geotrellis.spark.store.hadoop._
import geotrellis.util.MethodExtensions

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import _root_.io.circe.Decoder

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withHadoopCOGCollectionLayerReaderMethods(val self: HadoopCOGCollectionLayerReader.type) extends MethodExtensions[HadoopCOGCollectionLayerReader.type] {
    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGCollectionLayerReader =
      HadoopCOGCollectionLayerReader(HadoopAttributeStore(rootPath))
  }

  implicit class withHadoopCOGValueReaderMethods(val self: HadoopCOGValueReader.type) extends MethodExtensions[HadoopCOGValueReader.type] {
    def apply[
      K: Decoder: SpatialComponent: ClassTag,
      V <: CellGrid[Int]: GeoTiffReader
    ](attributeStore: HadoopAttributeStore, layerId: LayerId)(implicit sc: SparkContext): Reader[K, V] =
      new HadoopCOGValueReader(attributeStore, sc.hadoopConfiguration).reader[K, V](layerId)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGValueReader =
      HadoopCOGValueReader(HadoopAttributeStore(rootPath))
  }
}
