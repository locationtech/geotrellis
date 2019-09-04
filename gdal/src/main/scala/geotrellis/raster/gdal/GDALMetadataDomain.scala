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

package geotrellis.raster.gdal

/** https://github.com/geosolutions-it/imageio-ext/blob/1.3.2/library/gdalframework/src/main/java/it/geosolutions/imageio/gdalframework/GDALUtilities.java#L68 */
sealed trait GDALMetadataDomain {
  def name: String
  override def toString: String = name
}

object GDALMetadataDomain {
  def ALL = List(DefaultDomain, ImageStructureDomain, SubdatasetsDomain)
}

case object DefaultDomain extends GDALMetadataDomain {
  def name = ""
}

/** https://github.com/OSGeo/gdal/blob/bed760bfc8479348bc263d790730ef7f96b7d332/gdal/doc/source/development/rfc/rfc14_imagestructure.rst **/
case object ImageStructureDomain extends GDALMetadataDomain {
  def name = "IMAGE_STRUCTURE"
}
/** https://github.com/OSGeo/gdal/blob/6417552c7b3ef874f8306f83e798f979eb37b309/gdal/doc/source/drivers/raster/eedai.rst#subdatasets */
case object SubdatasetsDomain extends GDALMetadataDomain {
  def name = "SUBDATASETS"
}

case class UserDefinedDomain(name: String) extends GDALMetadataDomain
