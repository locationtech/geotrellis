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
  def bandCount = bandTags.size
  def apply(): Map[String, String] = headTags
  def apply(i: Int): Map[String, String] = bandTags(i)

  override
  def equals(o: Any): Boolean =
    o match {
      case other: Tags =>
        bandCount == other.bandCount &&
        (0 until bandCount).foldLeft(true) { case (acc, i) => acc & (apply(i) == other(i)) } &&
        apply().equals(other())
      case _ => false
    }

  override
  def hashCode =
    (headTags, bandTags).hashCode

  def toXml(): scala.xml.Elem = {
    val headTagsXml =
      headTags.toSeq.map { case (key, value) =>
        <Item name={key}>{value}</Item>
      }

    val bandTagsXml: Seq[scala.xml.Elem] =
      bandTags.zipWithIndex.flatMap { case (map, i) =>
        map.toSeq.map { case (key, value) =>
          <Item name={key} sample={i.toString}>{value}</Item>
        }
      }

    <GDALMetadata>
      {headTagsXml}
      {bandTagsXml}
    </GDALMetadata>
  }
}
