package geotrellis.raster.io.geotiff

object Tags {
  def empty: Tags = Tags(Map(), Array())

  def apply(headTags: Map[String, String], bandTags: Array[Map[String, String]]) = 
    new Tags(headTags, bandTags)

  /** For convenience, implicitly convert the Tags to the head Tags */
  implicit def tagsToHeadTags(tags: Tags): Map[String, String] =
    tags()
}

/** Tags are user data that the GeoTiff is tagged with.
  * While GDAL calls the data "metadata", we call them tags.
  * See the "Metadata" section here: http://www.gdal.org/gdal_datamodel.html
  */
class Tags(headTags: Map[String, String], bandTags: Array[Map[String, String]]) {
  def apply(): Map[String, String] = headTags
  def apply(i: Int): Map[String, String] = bandTags(i)
}
