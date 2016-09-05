package geotrellis.geotools

import geotrellis.proj4.CRS
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Feature, Geometry}
import org.opengis.feature.simple.SimpleFeature

trait FeatureToGeoMesaSimpleFeatureMethods[G <: Geometry, T] extends MethodExtensions[Feature[G, T]] {
  def toSimpleFeature(featureId: String, crs: Option[CRS] = None)(implicit transmute: T => Seq[(String, Any)]): SimpleFeature =
    GeometryToGeoMesaSimpleFeature(featureId, self.geom, crs, self.data)
}
