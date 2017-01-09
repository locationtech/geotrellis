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

package geotrellis.pointcloud.spark.tiling

import io.pdal._

import geotrellis.spark._
import geotrellis.pointcloud.spark._
import geotrellis.spark.tiling._

import org.apache.spark.rdd._
import org.apache.spark.Partitioner
import spire.syntax.cfor._
import com.vividsolutions.jts.{geom => jts}

import scala.collection.mutable

object CutPointCloud {
  case class Options(
    partitioner: Option[Partitioner] = None
  )

  object Options {
    def DEFAULT = Options()
  }

  def apply(rdd: RDD[Array[jts.Coordinate]], layoutDefinition: LayoutDefinition): RDD[(SpatialKey, Array[jts.Coordinate])] with Metadata[LayoutDefinition] =
    apply(rdd, layoutDefinition, Options.DEFAULT)

  def apply(rdd: RDD[Array[jts.Coordinate]], layoutDefinition: LayoutDefinition, options: Options): RDD[(SpatialKey, Array[jts.Coordinate])] with Metadata[LayoutDefinition] = {
    val mapTransform = layoutDefinition.mapTransform
    val (tileCols, tileRows) = layoutDefinition.tileLayout.tileDimensions
    val tilePoints = tileCols * tileRows

    val cut =
      rdd
        .flatMap { pointCloud =>
          var lastKey: SpatialKey = null
          val keysToPoints = mutable.Map[SpatialKey, mutable.ArrayBuffer[jts.Coordinate]]()
          val pointSize = pointCloud.length

          cfor(0)(_ < pointSize, _ + 1) { i =>
            val p = pointCloud(i)
            val key = mapTransform(p.x, p.y)
            if(key == lastKey) {
              keysToPoints(lastKey) += p
            } else if(keysToPoints.contains(key)) {
              keysToPoints(key) += p
              lastKey = key
            } else {
              keysToPoints(key) = mutable.ArrayBuffer(p)
              lastKey = key
            }
          }

          keysToPoints.map { case (k, v) => (k, v.toArray) }
        }

    val merged =
      options.partitioner match {
        case Some(partitioner) =>
          cut.reduceByKey(partitioner, { (p1, p2) => p1 union p2 })
        case None =>
          cut.reduceByKey { (p1, p2) => p1 union p2 }
      }

    ContextRDD(cut, layoutDefinition)
  }


  def apply(rdd: RDD[PointCloud], layoutDefinition: LayoutDefinition)(implicit d: DummyImplicit): RDD[(SpatialKey, PointCloud)] with Metadata[LayoutDefinition] =
    apply(rdd, layoutDefinition, Options.DEFAULT)

  def apply(rdd: RDD[PointCloud], layoutDefinition: LayoutDefinition, options: Options)(implicit d: DummyImplicit): RDD[(SpatialKey, PointCloud)] with Metadata[LayoutDefinition] = {
    val mapTransform = layoutDefinition.mapTransform
    val (tileCols, tileRows) = layoutDefinition.tileLayout.tileDimensions
    val tilePoints = tileCols * tileRows

    val cut =
      rdd
        .flatMap { pointCloud =>
          var lastKey: SpatialKey = null
          val keysToBytes = mutable.Map[SpatialKey, mutable.ArrayBuffer[Array[Byte]]]()
          val pointSize = pointCloud.pointSize

          val len = pointCloud.length
          cfor(0)(_ < len, _ + 1) { i =>
            val x = pointCloud.getX(i)
            val y = pointCloud.getY(i)
            val key = mapTransform(x, y)
            if(key == lastKey) {
              keysToBytes(lastKey) += pointCloud.get(i)
            } else if(keysToBytes.contains(key)) {
              keysToBytes(key) += pointCloud.get(i)
              lastKey = key
            } else {
              keysToBytes(key) = mutable.ArrayBuffer(pointCloud.get(i))
              lastKey = key
            }
          }

          keysToBytes.map { case (key, pointBytes) =>
            val arr = Array.ofDim[Byte](pointBytes.size * pointSize.toInt)
            var i = 0
            pointBytes.foreach { bytes =>
              var p = 0
              while(p < pointSize) {
                arr(i) = bytes(p)
                i += 1
                p += 1
              }
            }

            (key, PointCloud(arr, pointCloud.dimTypes))
          }
        }

    val merged =
      options.partitioner match {
        case Some(partitioner) =>
          cut.reduceByKey(partitioner, { (p1, p2) => p1 union p2 })
        case None =>
          cut.reduceByKey { (p1, p2) => p1 union p2 }
      }

    ContextRDD(merged, layoutDefinition)
  }
}
