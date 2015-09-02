# Cloud-removal

This code can be used to generate cloud-free satellite imagery from a set of images of a geographical location.

*Input* - An array of multiband RGB rasters.  
*Output* - A relatively cloud-free PNG of the given extent.

*cloudRemovalMultiBand* is the important function here. It takes in an array of *MultiBandTile*, which are the GeoTIFF tiles we need to operate on and an optional *threshold* parameter, which specifies the pixel intensity value below which the resultant cloudless-pixels' intensities would lie. The function returns a processed *MultiBandTile* that can be rendered as a PNG.

Here's an example of it's usage:

    def main(args: Array[String]) : Unit = {
        val dirRed = new File(args(0))
        val dirGreen = new File(args(1))
        val dirBlue = new File(args(2))
    
        val fileListRed = dirRed.listFiles.filter(_.isFile).toList.toArray
        val fileListGreen = dirGreen.listFiles.filter(_.isFile).toList.toArray
        val fileListBlue = dirBlue.listFiles.filter(_.isFile).toList.toArray
    
        val numImages = fileListRed.length
    
        assert(numImages == fileListBlue.length && numImages == fileListGreen.length)
    
        val multiBands = Array.ofDim[MultiBandTile](numImages)
    
        cfor(0)(_ < numImages, _ + 1) { i =>
          val red = SingleBandGeoTiff(fileListRed(i).toString).tile
          val green = SingleBandGeoTiff(fileListGreen(i).toString).tile
          val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile
    
          multiBands(i) = ArrayMultiBandTile(Array(red, green, blue))
        }
    
        val cloudless = cloudRemovalMultiBand(multiBands)
        cloudless.renderPng().write("/tmp/cloudless.png")
      }

