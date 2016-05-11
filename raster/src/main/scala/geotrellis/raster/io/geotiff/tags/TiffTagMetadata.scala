package geotrellis.raster.io.geotiff.tags

case class TiffTagMetadata(
  tag: Int,
  fieldType: Int,
  length: Int,
  offset: Int
)
