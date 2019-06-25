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

package geotrellis.geomesa.geotools

import geotrellis.spark.store.geomesa.conf.GeoMesaConfig
import geotrellis.proj4.{CRS => GCRS}
import geotrellis.util.annotations.experimental
import geotrellis.vector._
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental
object GeometryToGeoMesaSimpleFeature {

  val whenField: String = "when"
  val whereField: String = "the_geom"
  val indexDtg: String = SimpleFeatureTypes.Configs.DEFAULT_DATE_KEY

  lazy val featureTypeCache: Cache[String, SimpleFeatureType] =
    Scaffeine()
      .recordStats()
      .maximumSize(GeoMesaConfig.featureTypeCacheSize)
      .build[String, SimpleFeatureType]()

  /** $experimental */
  @experimental
  def apply(featureName: String, geom: Geometry, featureId: Option[String], crs: Option[GCRS], data: Seq[(String, Any)]): SimpleFeature = {
    val sft = featureTypeCache.get(featureName, { key =>
      val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)

      sftb.setName(featureName)
      crs match {
        case Some(crs) => sftb.setSRS(s"EPSG:${crs.epsgCode.get}")
        case None =>
      }
      geom match {
        case pt: Point => sftb.add(whereField, classOf[Point])
        case ln: LineString => sftb.add(whereField, classOf[LineString])
        case pg: Polygon => sftb.add(whereField, classOf[Polygon])
        case mp: MultiPoint => sftb.add(whereField, classOf[MultiPoint])
        case ml: MultiLineString => sftb.add(whereField, classOf[MultiLineString])
        case mp: MultiPolygon => sftb.add(whereField, classOf[MultiPolygon])
        case g: Geometry => throw new Exception(s"Unhandled Geometry type $g")
      }
      sftb.setDefaultGeometry(whereField)
      data.foreach({ case (key, value) => sftb
        .minOccurs(1).maxOccurs(1).nillable(false)
        .add(key, value.getClass)
      })
      sftb.buildFeatureType
    })

    if(data.map(_._1).contains(whenField)) sft.getUserData.put(indexDtg, whenField) // when field is date
    val sfb = new SimpleFeatureBuilder(sft)

    geom match {
      case pt: Point => sfb.add(pt)
      case ln: LineString => sfb.add(ln)
      case pg: Polygon => sfb.add(pg)
      case mp: MultiPoint => sfb.add(mp)
      case ml: MultiLineString => sfb.add(ml)
      case mp: MultiPolygon => sfb.add(mp)
      case g: Geometry => throw new Exception(s"Unhandled GeoTrellis Geometry $g")
    }
    data.foreach({ case (key, value) => sfb.add(value) })

    sfb.buildFeature(featureId.orNull)
  }
}
