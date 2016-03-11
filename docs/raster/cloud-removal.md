# Cloud-removal

A recurring problem with raster processing is the need to remove clouds.
With this in mind, code for generating cloud-free satellite imagery from
a set of images of a geographical location is included within
`geotrellis.raster`

`cloudRemovalMultiband` is the important function here. It takes in
an array of `MultibandTile`, which are the GeoTIFF tiles we need to
operate on and an optional `threshold` parameter (which specifies the
pixel intensity value below which the resultant cloudless-pixels'
intensities would lie). The function returns a processed `MultibandTile`
that can be rendered as a PNG.

Here's an example of its usage:

```scala
import geotrellis.raster._
import spire.syntax.cfor._

def main(args: Array[String]) : Unit = {
  val dirRed = new File(args(0))
  val dirGreen = new File(args(1))
  val dirBlue = new File(args(2))

  val fileListRed = dirRed.listFiles.filter(_.isFile).toList.toArray
  val fileListGreen = dirGreen.listFiles.filter(_.isFile).toList.toArray
  val fileListBlue = dirBlue.listFiles.filter(_.isFile).toList.toArray

  val numImages = fileListRed.length

  // Should have an equal number of R, G, B tiles
  assert(numImages == fileListBlue.length && numImages == fileListGreen.length)

  val multibands = Array.ofDim[MultibandTile](numImages)

  cfor(0)(_ < numImages, _ + 1) { i =>
    val red = SinglebandGeoTiff(fileListRed(i).toString).tile
    val green = SinglebandGeoTiff(fileListGreen(i).toString).tile
    val blue = SinglebandGeoTiff(fileListBlue(i).toString).tile

    multibands(i) = ArrayMultibandTile(Array(red, green, blue))
  }

  val cloudless = cloudRemovalMultiband(multibands)
  cloudless.renderPng().write("/tmp/cloudless.png")
}
```
