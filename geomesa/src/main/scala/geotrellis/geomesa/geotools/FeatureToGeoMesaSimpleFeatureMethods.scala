package geotrellis.geomesa.geotools

import geotrellis.proj4.CRS
import geotrellis.util.annotations.experimental
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Feature, Geometry}

import org.opengis.feature.simple.SimpleFeature

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental trait FeatureToGeoMesaSimpleFeatureMethods[G <: Geometry, T] extends MethodExtensions[Feature[G, T]] {

  /** $experimental */
  @experimental def toSimpleFeature(featureName: String, featureId: Option[String] = None, crs: Option[CRS] = None)(implicit transmute: T => Seq[(String, Any)]): SimpleFeature =
    GeometryToGeoMesaSimpleFeature(featureName, self.geom, featureId, crs, self.data)
}
