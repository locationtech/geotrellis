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

package geotrellis.spark.io.hadoop.geotiff

import geotrellis.vector.ProjectedExtent
import geotrellis.spark.tiling._
import geotrellis.proj4.{CRS, WebMercator}

import scala.collection.mutable
import com.vividsolutions.jts.index.strtree.STRtree
import java.io._

import collection.JavaConverters._

case class GeoTiffMetadataTree[T](index: mutable.Map[String, STRtree], crs: CRS) {
  def insert(name: String, projectedExtent: ProjectedExtent)(md: T): Unit =
    index(name).insert(projectedExtent.reproject(crs).jtsEnvelope, md)

  def query(name: String, projectedExtent: ProjectedExtent): List[T] = {
    val res = index(name)
      .query(projectedExtent.reproject(crs).jtsEnvelope)
      .asScala
      .toList
      .asInstanceOf[List[T]]

    res
  }

  def query(projectedExtent: ProjectedExtent): List[T] = {
    val res = index.values.flatMap {
      _.query(projectedExtent.reproject(crs).jtsEnvelope)
        .asScala
        .toList
        .asInstanceOf[List[T]]
    }.toList

    res
  }

  def query: List[T] = {
    val res = index.values.flatMap {
      _.query(crs.worldExtent.jtsEnvelope)
        .asScala
        .toList
        .asInstanceOf[List[T]]
    }.toList

    res
  }

  def serialize: OutputStream = {
    val byteArrayStream = new ByteArrayOutputStream
    val out = new ObjectOutputStream(byteArrayStream)
    out.writeObject(index)
    out
  }

  def deserialize(is: InputStream): STRtree = {
    val in = new ObjectInputStream(is)
    val res = in.readObject.asInstanceOf[STRtree]
    in.close()
    res
  }
}

object GeoTiffMetadataTree {
  def fromGeoTiffMetadataSeq(list: Seq[GeoTiffMetadata], crs: CRS = WebMercator): GeoTiffMetadataTree[GeoTiffMetadata] = {
    val map = mutable.Map[String, STRtree]()
    list.foreach { md =>
      val index = map.getOrElseUpdate(md.name, new STRtree())
      index.insert(md.projectedExtent.reproject(crs).jtsEnvelope, md)
    }

    GeoTiffMetadataTree(map, crs)
  }

}
