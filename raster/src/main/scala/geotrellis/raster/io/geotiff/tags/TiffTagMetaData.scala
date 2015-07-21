package geotrellis.raster.io.geotiff.tags

case class TiffTagMetaData(
  tag: Int,
  fieldType: Int,
  length: Int,
  offset: Int
)
