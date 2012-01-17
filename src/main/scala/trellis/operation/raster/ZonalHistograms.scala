package trellis.operation

import scala.math.{min, max}

import trellis._
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._
import trellis.IntRaster
import trellis.stat.{Histogram, ArrayHistogram, MapHistogram, CompressedArrayHistogram, Statistics}
import trellis.geometry.Polygon

/**
  * Given a raster and an array of polygons, return a histogram summary of the cells
  * within each polygon.
  */
case class ZonalHistograms(data: Op[IntRaster],
                           zones: Op[IntRaster],
                           zonesArraySize: Int,
                           histArraySize: Int) extends Op[Array[Histogram]] {

  def _run(context:Context) = runAsync(List(data, zones))

  val nextSteps:Steps = {
    case dataRaster :: zoneRaster :: Nil => { 
      step2(dataRaster.asInstanceOf[IntRaster], zoneRaster.asInstanceOf[IntRaster])
    }
  }

  def step2(raster:IntRaster, zones: IntRaster) = {
    // build our map to hold results
    val histmap = Array.ofDim[Histogram](zonesArraySize)
    for(i <- 0 until histmap.length) {
      histmap(i) = ArrayHistogram(histArraySize)
    }

    // dereference some useful variables
    val geo   = raster.rasterExtent
    val rdata = raster.data
    val rows  = geo.rows
    val cols  = geo.cols

    // calculate the bounding box
    var xmin = geo.extent.xmin
    var ymin = geo.extent.ymin
    var xmax = geo.extent.xmax
    var ymax = geo.extent.ymax

    // save the bounding box as grid coordinates
    val (col1, row1) = geo.mapToGrid(xmin, ymin)
    val (col2, row2) = geo.mapToGrid(xmax, ymax)

    val zdata = zones.data

    // iterate over the cells in our bounding box; determine its zone, then
    // looking in the raster for a value to add to the zonal histogram.
    var row = row1
    while (row < row2) {
      var col = col1
      while (col < col2) {
        val i     = row * cols + col
        val value = rdata(i)
        if (value != NODATA) {
          val zone  = zdata(i)
          if (zone != NODATA) {
            val histogram = histmap(zone)
            histogram.countItem(value)
          }
        }
        col += 1
      }

      row += 1
    }

    // return an immutable mapping
    Result(histmap)
  }

}
