package geotrellis.geotools

import geotrellis.vector.Geometry
import geotrellis.proj4.{CRS => GCRS, WebMercator}

import com.vividsolutions.jts.{geom => jts}
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.locationtech.geomesa.accumulo.index.Constants

import scala.reflect._

object GeoMesaSimpleFeatureType {

  val whenField  = "when"
  val whereField = "where"

  def apply[G <: Geometry: ClassTag](featureName: String, crs: Option[GCRS] = Some(WebMercator), temporal: Boolean = false) = {
    val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)

    sftb.setName(featureName)
    crs.foreach { crs => sftb.setSRS(s"EPSG:${crs.epsgCode.get}") }
    classTag[G].toString.split("\\.").last match {
      case "Point" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.Point])
      case "Line" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.LineString])
      case "Polygon" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.Polygon])
      case "MultiPoint" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.MultiPoint])
      case "MultiLine" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.MultiLineString])
      case "MultiPolygon" => sftb.add(GeometryToGeoMesaSimpleFeature.whereField, classOf[jts.MultiPolygon])
      case g => throw new Exception(s"Unhandled GeoTrellis Geometry $g")
    }
    sftb.setDefaultGeometry(whereField)
    if(temporal) sftb.add(whenField, classOf[java.util.Date])
    val sft = sftb.buildFeatureType
    if(temporal) sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, whenField) // when field is date
    sft.getUserData.put("geomesa.mixed.geometries", java.lang.Boolean.TRUE) // allow GeoMesa to index points and extents together
    sft
  }
}
