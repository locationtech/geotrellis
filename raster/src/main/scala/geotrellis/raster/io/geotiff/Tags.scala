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

package geotrellis.raster.io.geotiff

object Tags {
  def empty: Tags = Tags(Map(), List())

  final val AREA_OR_POINT = "AREA_OR_POINT"
  final val TIFFTAG_DATETIME = "TIFFTAG_DATETIME"
}

/** Tags are user data that the GeoTiff is tagged with.
  * While GDAL calls the data "metadata", we call them tags.
  * See the "Metadata" section here: http://www.gdal.org/gdal_datamodel.html
  */
case class Tags(headTags: Map[String, String], bandTags: List[Map[String, String]]) {
  def bandCount = bandTags.size

  override
  def equals(o: Any): Boolean =
    o match {
      case other: Tags =>
        bandCount == other.bandCount &&
        (0 until bandCount).foldLeft(true) { case (acc, i) => acc & (bandTags(i) == other.bandTags(i)) } &&
        headTags.equals(other.headTags)
      case _ => false
    }

  override
  def hashCode =
    (headTags, bandTags).hashCode

  def toXml(): scala.xml.Elem = {
    val headTagsXml =
      headTags.toSeq.map { case (key, value) =>
        <Item name={key}>{value}</Item>
      }

    val bandTagsXml: Seq[scala.xml.Elem] =
      bandTags.zipWithIndex.flatMap { case (map, i) =>
        map.toSeq.map { case (key, value) =>
          <Item name={key} sample={i.toString}>{value}</Item>
        }
      }

    <GDALMetadata>
      {headTagsXml}
      {bandTagsXml}
    </GDALMetadata>
  }
}
