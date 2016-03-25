package geotrellis.vector
package prepared

import geotrellis.util.MethodExtensions

/** Extension methods for [[PreparedGeometry]] instances */
trait PreparedGeometryMethods[G <: Geometry] extends MethodExtensions[G] {
  def prepare: PreparedGeometry[G] = PreparedGeometry(self)
}
