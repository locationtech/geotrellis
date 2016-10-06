package geotrellis.geomesa.geotools

import geotrellis.vector.Geometry
import geotrellis.proj4.{WebMercator, CRS => GCRS}
import geotrellis.spark.util.cache.LRUCache

import com.vividsolutions.jts.{geom => jts}
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.locationtech.geomesa.accumulo.index.Constants
import org.opengis.feature.simple.SimpleFeatureType
import com.typesafe.config.ConfigFactory

import scala.reflect._

object GeoMesaSimpleFeatureType {

  val whenField  = GeometryToGeoMesaSimpleFeature.whenField
  val whereField = GeometryToGeoMesaSimpleFeature.whereField

  lazy val featureTypeCache =
    new LRUCache[String, SimpleFeatureType](
      maxSize = ConfigFactory.load().getInt("geotrellis.geomesa.featureTypeCacheSize"),
      sizeOf  = {x => 1l}
    )

  def apply[G <: Geometry: ClassTag](featureName: String, crs: Option[GCRS] = Some(WebMercator), temporal: Boolean = false): SimpleFeatureType = {
    featureTypeCache.getOrInsert(featureName, {
      val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)

      sftb.setName(featureName)
      crs.foreach { crs => sftb.setSRS(s"EPSG:${crs.epsgCode.get}") }
      classTag[G].runtimeClass.getName match {
        case "geotrellis.vector.Point" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.Point])
        case "geotrellis.vector.Line" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.LineString])
        case "geotrellis.vector.Polygon" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.Polygon])
        case "geotrellis.vector.MultiPoint" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.MultiPoint])
        case "geotrellis.vector.MultiLine" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.MultiLineString])
        case "geotrellis.vector.MultiPolygon" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.MultiPolygon])
        case g => throw new Exception(s"Unhandled GeoTrellis Geometry $g")
      }
      sftb.setDefaultGeometry(whereField)
      if (temporal) sftb.add(whenField, classOf[java.util.Date])
      val sft = sftb.buildFeatureType
      if (temporal) sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, whenField) // when field is date
      sft
    })
  }
}
