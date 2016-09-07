package geotrellis.geotools

import geotrellis.util.MethodExtensions
import geotrellis.vector.{Feature, Geometry}

object GeoMesaImplicits extends GeoMesaImplicits

trait GeoMesaImplicits {
  implicit class withFeatureToGeoMesaSimpleFeatureMethods[G <: Geometry, T](val self: Feature[G, T])
    extends MethodExtensions[Feature[G, T]]
      with FeatureToGeoMesaSimpleFeatureMethods[G, T]
}
