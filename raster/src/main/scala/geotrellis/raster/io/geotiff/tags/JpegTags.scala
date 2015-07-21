package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class JpegTags(
  jpegTables: Option[Array[Byte]] = None,
  jpegProc: Option[Int] = None,
  jpegInterchangeFormat: Option[Long] = None,
  jpegInterchangeFormatLength: Option[Long] = None,
  jpegRestartInterval: Option[Int] = None,
  jpegLosslessPredictors: Option[Array[Int]] = None,
  jpegPointTransforms: Option[Array[Int]] = None,
  jpegQTables: Option[Array[Long]] = None,
  jpegDCTables: Option[Array[Long]] = None,
  jpegACTables: Option[Array[Long]] = None
)
