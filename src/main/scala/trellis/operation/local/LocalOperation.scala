package trellis.operation


/*
 * Local operations involve each individual value in a raster without information
 * about other values in the raster. 
 */
trait LocalOperation extends IntRasterOperation { 
  var childTime:Long = 0
  def getRasters: Seq[IntRasterOperation]
  def childOperations = getRasters
  override def exclusiveTime = totalTime - childTime
}
