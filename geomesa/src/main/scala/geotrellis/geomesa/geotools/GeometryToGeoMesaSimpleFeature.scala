package geotrellis.geomesa.geotools

import geotrellis.proj4.{CRS => GCRS}
import geotrellis.spark.util.cache.LRUCache
import geotrellis.util.annotations.experimental
import geotrellis.vector.{Geometry, Line, MultiLine, MultiPoint, MultiPolygon, Point, Polygon}

import com.vividsolutions.jts.{geom => jts}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.locationtech.geomesa.accumulo.index.Constants
import com.typesafe.config.ConfigFactory

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeometryToGeoMesaSimpleFeature {

  val whenField  = "when"
  val whereField = "where"

  lazy val featureTypeCache =
    new LRUCache[String, SimpleFeatureType](
      maxSize = ConfigFactory.load().getInt("geotrellis.geomesa.featureTypeCacheSize"),
      sizeOf  = {x => 1l}
    )

  /** $experimental */
  @experimental def apply(featureName: String, geom: Geometry, featureId: Option[String], crs: Option[GCRS], data: Seq[(String, Any)]): SimpleFeature = {
    val sft = featureTypeCache.getOrInsert(featureName, {
      val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)

      sftb.setName(featureName)
      crs match {
        case Some(crs) => sftb.setSRS(s"EPSG:${crs.epsgCode.get}")
        case None =>
      }
      geom match {
        case pt: Point => sftb.add(whereField, classOf[jts.Point])
        case ln: Line => sftb.add(whereField, classOf[jts.LineString])
        case pg: Polygon => sftb.add(whereField, classOf[jts.Polygon])
        case mp: MultiPoint => sftb.add(whereField, classOf[jts.MultiPoint])
        case ml: MultiLine => sftb.add(whereField, classOf[jts.MultiLineString])
        case mp: MultiPolygon => sftb.add(whereField, classOf[jts.MultiPolygon])
        case g: Geometry => throw new Exception(s"Unhandled GeoTrellis Geometry $g")
      }
      sftb.setDefaultGeometry(whereField)
      data.foreach({ case (key, value) => sftb
        .minOccurs(1).maxOccurs(1).nillable(false)
        .add(key, value.getClass)
      })
      sftb.buildFeatureType
    })

    if(data.map(_._1).contains(whenField)) sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, whenField) // when field is date
    val sfb = new SimpleFeatureBuilder(sft)

    geom match {
      case Point(pt) => sfb.add(pt)
      case Line(ln) => sfb.add(ln)
      case Polygon(pg) => sfb.add(pg)
      case MultiPoint(mp) => sfb.add(mp)
      case MultiLine(ml) => sfb.add(ml)
      case MultiPolygon(mp) => sfb.add(mp)
      case g: Geometry => throw new Exception(s"Unhandled Geotrellis Geometry $g")
    }
    data.foreach({ case (key, value) => sfb.add(value) })

    sfb.buildFeature(featureId.orNull)
  }
}
