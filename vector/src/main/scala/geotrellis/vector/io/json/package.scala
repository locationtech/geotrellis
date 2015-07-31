package geotrellis.vector.io

import geotrellis.vector._
import spray.json._
import spray.json.JsonFormat
import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer

package object json extends GeoJsonSupport {

  implicit class GeometriesToGeoJson(val geoms: Traversable[Geometry]) extends AnyVal {
    def toGeoJson: String = {
      GeometryCollection(geoms).toJson.compactPrint
    }
  }

  implicit class ExtentsToGeoJson(val extent: Extent) extends AnyVal {
    def toGeoJson: String = {
      extent.toPolygon.toGeoJson
    }
  }

  implicit class FeaturesToGeoJson[G <: Geometry, D: JsonWriter](features: Traversable[Feature[G, D]]) {
    def toGeoJson: String = {
      JsonFeatureCollection(features).toJson.compactPrint
    }
  }

  implicit class RichGeometry(val geom: Geometry) extends AnyVal {
    def toGeoJson: String = geom.toJson.compactPrint

    def withCrs(crs: CRS) = WithCrs(geom, crs)
  }

  implicit class RichFeature[G <: Geometry, D: JsonWriter](feature: Feature[G, D]) {
    def toGeoJson: String = writeFeatureJson(feature).compactPrint

    def withCrs(crs: CRS) = WithCrs(feature, crs)
  }

  implicit class RichString(val s: String) extends AnyVal {

    /**
     * This expects all the types to line up, throws if not.
     */
    def parseGeoJson[T: JsonReader] = s.parseJson.convertTo[T]

    /**
     * maybe empty, a number of pointfeatures in a featurecollection should
     * be extracted as a Seq[Point], not a GeometryCollection (parseGeoJson would do that instead)
     */
    def extractGeometries[G <: Geometry : ClassTag]: Seq[G] = {

      val clazz = implicitly[ClassTag[G]].runtimeClass
      var buffer = ArrayBuffer[G]()

      try {
        val fc = s.parseGeoJson[JsonFeatureCollection]
        val feats = fc.getAllPoints() ++
          fc.getAllLines() ++
          fc.getAllPolygons() ++
          fc.getAllMultiPoints() ++
          fc.getAllMultiLines() ++
          fc.getAllMultiPolygons()
        feats.foreach {
          geom => geom match {
            case geom: G if clazz.isInstance(geom) => buffer += geom
            case _ =>
          }

        }
      } catch {
        case ex: DeserializationException => // empty
        // everything else throw something
      }

      try {
        val gc = s.parseGeoJson[GeometryCollection]
        gc.geometries.foreach {
          geom => geom match {
            case geom: G if clazz.isInstance(geom) => buffer += geom
            case _ =>
          }
        }
      } catch {
        case ex: DeserializationException => // empty
        // enything else throw something
      }

      try {
        val geom = s.parseGeoJson[Point]
        geom match {
          case geom: G if clazz.isInstance(geom) => buffer += geom
          case _ =>
        }
      } catch {
        case ex: DeserializationException => // empty
        // enything else throw something
      }

      try {
        val geom = s.parseGeoJson[Line]
        geom match {
          case geom: G if clazz.isInstance(geom) => buffer += geom
          case _ =>
        }
      } catch {
        case ex: DeserializationException => // empty
        // enything else throw something
      }

      try {
        val geom = s.parseGeoJson[Polygon]
        geom match {
          case geom: G if clazz.isInstance(geom) => buffer += geom
          case _ =>
        }
      } catch {
        case ex: DeserializationException => // empty
        // enything else throw something
      }

      try {
        val geom = s.parseGeoJson[MultiPoint]
        geom match {
          case geom: G if clazz.isInstance(geom) => buffer += geom
          case _ =>
        }
      } catch {
        case ex: DeserializationException => // empty
        // enything else throw something
      }

      try {
        val geom = s.parseGeoJson[MultiLine]
        geom match {
          case geom: G if clazz.isInstance(geom) => buffer += geom
          case _ =>
        }
      } catch {
        case ex: DeserializationException => // empty
        // enything else throw something
      }
      try {
        val geom = s.parseGeoJson[MultiPolygon]
        geom match {
          case geom: G if clazz.isInstance(geom) => buffer += geom
          case _ =>
        }
      } catch {
        case ex: DeserializationException => // empty
        // enything else throw something
      }

      buffer.toSeq
    }

    /**
     * maybe empty, a number of pointfeatures in a featurecollection should
     * be extracted as a Seq[PointFeature], not a FeatureCollection (parseGeoJson would do that instead)
     */
    def extractFeatures[G <: Geometry, D]: Seq[Feature[G, D]] = {

      val geomClazz = implicitly[ClassTag[G]].runtimeClass
      var points = ArrayBuffer[Feature[Point, D]]()
      var lines = ArrayBuffer[Feature[Line, D]]()
      var polygons = ArrayBuffer[Feature[Polygon, D]]()
      var multipoints = ArrayBuffer[Feature[MultiPoint, D]]()
      var multilines = ArrayBuffer[Feature[MultiLine, D]]()
      var multipolygons = ArrayBuffer[Feature[MultiPolygon, D]]()

      geomClazz match {
        case geomClazz: Class[G] if geomClazz.isInstance(Point) => {
          try {
            val fc = s.parseGeoJson[JsonFeatureCollection]
            points ++= fc.getAllPointFeatures[D]().map(pf => Feature[Point, D](pf.geom, pf.data))
          } catch {
            case ex: DeserializationException => // empty
            // everything else throw something
          }
        }
        case geomClazz: Class[G] if geomClazz.isInstance(Line) => {
          try {
            val fc = s.parseGeoJson[JsonFeatureCollection]
            lines ++= fc.getAllLineFeatures[D]().map(pf => Feature[Line, D](pf.geom, pf.data))
          } catch {
            case ex: DeserializationException => // empty
            // everything else throw something
          }
        }
        case geomClazz: Class[G] if geomClazz.isInstance(Polygon) => {
          try {
            val fc = s.parseGeoJson[JsonFeatureCollection]
            polygons ++= fc.getAllPolygonFeatures[D]().map(pf => Feature[Polygon, D](pf.geom, pf.data))
          } catch {
            case ex: DeserializationException => // empty
            // everything else throw something
          }
        }
        case geomClazz: Class[G] if geomClazz.isInstance(MultiPoint) => {
          try {
            val fc = s.parseGeoJson[JsonFeatureCollection]
            multipoints ++= fc.getAllMultiPointFeatures[D]().map(pf => Feature[MultiPoint, D](pf.geom, pf.data))
          } catch {
            case ex: DeserializationException => // empty
            // everything else throw something
          }
        }
        case geomClazz: Class[G] if geomClazz.isInstance(MultiLine) => {
          try {
            val fc = s.parseGeoJson[JsonFeatureCollection]
            multilines ++= fc.getAllMultiLineFeatures[D]().map(pf => Feature[MultiLine, D](pf.geom, pf.data))
          } catch {
            case ex: DeserializationException => // empty
            // everything else throw something
          }
        }
        case geomClazz: Class[G] if geomClazz.isInstance(MultiPolygon) => {
          try {
            val fc = s.parseGeoJson[JsonFeatureCollection]
            multipolygons ++= fc.getAllMultiPolygonFeatures[D]().map(pf => Feature[MultiPolygon, D](pf.geom, pf.data))
          } catch {
            case ex: DeserializationException => // empty
            // everything else throw something
          }
          try {
            val feat = s.parseGeoJson[MultiPolygonFeature[D]]
            multipolygons += feat
          } catch {
            case ex: DeserializationException => // empty
            // everything else throw something
          }
        }
      }

      var buffer = ArrayBuffer[Feature[G, D]]()
      buffer
    }
  }

}
