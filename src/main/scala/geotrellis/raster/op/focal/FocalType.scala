package geotrellis.raster.op.focal

sealed trait FocalType

case object Aggregated extends FocalType
case object Columnar extends FocalType
case object Default extends FocalType
