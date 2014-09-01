package geotrellis.proj4.proj

import geotrellis.proj4._

import org.osgeo.proj4j.proj.Projection

trait ProjectionBuilder {
  def setProj4Params(params: ProjParams): ProjectionBuilder

  def build(): Projection
}
