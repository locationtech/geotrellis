package geotrellis.raster.matching

import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions


trait SinglebandMatchingMethods extends MethodExtensions[Tile] {

  def matchHistogram(targetHistogram: StreamingHistogram): Tile =
    HistogramMatching(self, targetHistogram)

  def matchHistogram(sourceHistogram: StreamingHistogram, targetHistogram: StreamingHistogram): Tile =
    HistogramMatching(self, sourceHistogram, targetHistogram)

}
