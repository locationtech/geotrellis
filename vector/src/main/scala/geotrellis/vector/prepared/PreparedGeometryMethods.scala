package geotrellis.vector
package prepared

import geotrellis.util.MethodExtensions

trait PreparedGeometryMethods[G <: Geometry] extends MethodExtensions[G] {
  def prepare: PreparedGeometry[G] = PreparedGeometry(self)
}
