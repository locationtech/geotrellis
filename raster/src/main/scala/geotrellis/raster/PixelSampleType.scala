package geotrellis.raster


/**
  * The base class for the two concrete [[PixelSampleType]]s.
  */
abstract sealed class PixelSampleType

/**
  * Type encoding the fact that pixels should be treated as points.
  */
case object PixelIsPoint extends PixelSampleType

/**
  * Type encoding the fact that pixels should be treated as
  * suitably-tiny extents.
  */
case object PixelIsArea extends PixelSampleType
