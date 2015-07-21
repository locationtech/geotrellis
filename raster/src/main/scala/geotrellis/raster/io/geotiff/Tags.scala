package geotrellis.raster.io.geotiff

object Tags {
  def empty: Tags = Tags(Map(), List())
}

/** Tags are user data that the GeoTiff is tagged with.
  * While GDAL calls the data "metadata", we call them tags.
  * See the "Metadata" section here: http://www.gdal.org/gdal_datamodel.html
  */
case class Tags(headTags: Map[String, String], bandTags: List[Map[String, String]]) {
  def bandCount = bandTags.size

  override
  def equals(o: Any): Boolean =
    o match {
      case other: Tags =>
        bandCount == other.bandCount &&
        (0 until bandCount).foldLeft(true) { case (acc, i) => acc & (bandTags(i) == other.bandTags(i)) } &&
        headTags.equals(other.headTags)
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
