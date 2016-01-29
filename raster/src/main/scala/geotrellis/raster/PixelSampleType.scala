package geotrellis.raster

abstract sealed class PixelSampleType

case object PixelIsPoint extends PixelSampleType
case object PixelIsArea extends PixelSampleType
