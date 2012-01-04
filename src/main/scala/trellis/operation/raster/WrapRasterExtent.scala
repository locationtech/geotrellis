package trellis.operation

import trellis.RasterExtent
import trellis.process._


/**
  * Return a hard-coded [[trellis.geoattrs.RasterExtent]]. 
  */ 
case class WrapRasterExtent(geo:RasterExtent)  extends SimpleOperation[RasterExtent]{
  def _value(context:Context) = geo
}
