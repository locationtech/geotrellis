/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.io.hadoop

import geotrellis.tiling.SpatialComponent
import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render.{Jpg, Png}
import geotrellis.raster.resample._
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.hadoop._
import geotrellis.spark.io._
import geotrellis.spark.util.KryoSerializer
import geotrellis.util.MethodExtensions

import spray.json._
import spray.json.DefaultJsonProtocol._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class HadoopSparkContextMethodsWrapper(val sc: SparkContext) extends HadoopSparkContextMethods
  implicit class withSaveBytesToHadoopMethods[K](rdd: RDD[(K, Array[Byte])]) extends SaveBytesToHadoopMethods[K](rdd)
  implicit class withSaveToHadoopMethods[K,V](rdd: RDD[(K,V)]) extends SaveToHadoopMethods[K, V](rdd)

  implicit class withJpgHadoopSparkWriteMethods(val self: Jpg) extends JpgHadoopSparkWriteMethods(self)

  implicit class withPngHadoopSparkWriteMethods(val self: Png) extends PngHadoopSparkWriteMethods(self)

  implicit class withGeoTiffSparkHadoopWriteMethods[T <: CellGrid[Int]](val self: GeoTiff[T]) extends GeoTiffHadoopSparkWriteMethods[T](self)

  implicit class withHadoopAttributeStoreMethods(val self: HadoopAttributeStore.type) extends MethodExtensions[HadoopAttributeStore.type] {
    def apply(rootPath: String)(implicit sc: SparkContext): HadoopAttributeStore =
      apply(new Path(rootPath))(sc)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopAttributeStore =
      HadoopAttributeStore(rootPath, sc.hadoopConfiguration)
  }

  implicit class withHadoopLayerCopierMethods(val self: HadoopLayerCopier.type) extends MethodExtensions[HadoopLayerCopier.type] {
    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerCopier =
      HadoopLayerCopier.apply(rootPath, HadoopAttributeStore(rootPath, sc.hadoopConfiguration))
  }

  implicit class withHadoopLayerDeleterMethods(val self: HadoopLayerDeleter.type) extends MethodExtensions[HadoopLayerDeleter.type] {
    def apply(attributeStore: AttributeStore)(implicit sc: SparkContext): HadoopLayerDeleter =
      HadoopLayerDeleter(attributeStore, sc.hadoopConfiguration)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerDeleter =
      HadoopLayerDeleter(HadoopAttributeStore(rootPath, new Configuration), sc.hadoopConfiguration)
  }

  implicit class withHadoopCollectionLayerReaderMethods(val self: HadoopCollectionLayerReader.type) extends MethodExtensions[HadoopCollectionLayerReader.type] {
    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCollectionLayerReader =
      new HadoopCollectionLayerReader(HadoopAttributeStore(rootPath), sc.hadoopConfiguration)

    def apply(attributeStore: AttributeStore)(implicit sc: SparkContext): HadoopCollectionLayerReader =
      new HadoopCollectionLayerReader(attributeStore, sc.hadoopConfiguration)
  }

  implicit class withHadoopValueReaderMethods(val self: HadoopValueReader.type) extends MethodExtensions[HadoopValueReader.type] {
    def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
      attributeStore: AttributeStore,
      layerId: LayerId
    )(implicit sc: SparkContext): Reader[K, V] =
      new HadoopValueReader(attributeStore, sc.hadoopConfiguration).reader[K, V](layerId)

    def apply[K: AvroRecordCodec: JsonFormat: SpatialComponent: ClassTag, V <: CellGrid[Int]: AvroRecordCodec: ? => TileResampleMethods[V]](
      attributeStore: AttributeStore,
      layerId: LayerId,
      resampleMethod: ResampleMethod
    )(implicit sc: SparkContext): Reader[K, V] =
      new HadoopValueReader(attributeStore, sc.hadoopConfiguration).overzoomingReader[K, V](layerId, resampleMethod)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopValueReader =
      HadoopValueReader(HadoopAttributeStore(rootPath, sc.hadoopConfiguration))
  }

  implicit class withHadoopSparkConfigurationMethods(val self: Configuration) extends MethodExtensions[Configuration] {
    def setSerialized[T: ClassTag](key: String, value: T): Unit = {
      val ser = KryoSerializer.serialize(value)
      self.set(key, new String(ser.map(_.toChar)))
    }

    def getSerialized[T: ClassTag](key: String): T = {
      val s = self.get(key)
      KryoSerializer.deserialize(s.toCharArray.map(_.toByte))
    }

    def getSerializedOption[T: ClassTag](key: String): Option[T] = {
      val s = self.get(key)
      if(s == null) {
        None
      } else {
        Some(KryoSerializer.deserialize(s.toCharArray.map(_.toByte)))
      }
    }
  }
}
