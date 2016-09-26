package geotrellis.geomesa.geotools

import geotrellis.proj4.CRS
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Feature, Geometry}
import org.opengis.feature.simple.SimpleFeature

trait FeatureToGeoMesaSimpleFeatureMethods[G <: Geometry, T] extends MethodExtensions[Feature[G, T]] {
  def toSimpleFeature(featureName: String, featureId: Option[String] = None, crs: Option[CRS] = None)(implicit transmute: T => Seq[(String, Any)]): SimpleFeature =
    GeometryToGeoMesaSimpleFeature(featureName, self.geom, featureId, crs, self.data)
}
