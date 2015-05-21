package geotrellis.spark

import geotrellis.raster.GridBounds
import geotrellis.vector.Extent

import com.github.nscala_time.time.Imports._

trait RasterRDDFilter[K, T]{
  def apply(metadata: RasterMetaData, kb: KeyBounds[K], thing: T): Option[KeyBounds[K]]
}





object RasterRDDFilter{

  implicit def gridBoundsFilter[K: SpatialComponent: Boundable] = new RasterRDDFilter[K, GridBounds] {
    def apply(metadata: RasterMetaData, kb: KeyBounds[K], bounds: GridBounds): Option[KeyBounds[K]] = {
      val queryBounds = KeyBounds(
        kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
        kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))

      implicitly[Boundable[K]].intersect(queryBounds, kb)
    }
  }

  implicit def extentFilter[K: SpatialComponent: Boundable] = new RasterRDDFilter[K, Extent] {
    def apply(metadata: RasterMetaData, kb: KeyBounds[K], extent: Extent): Option[KeyBounds[K]] = {      
      val bounds = metadata.mapTransform(extent)

      val queryBounds = KeyBounds(
        kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
        kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))

      implicitly[Boundable[K]].intersect(queryBounds, kb)
    }
  }

  implicit def timeFilter[K: TemporalComponent: Boundable] = new RasterRDDFilter[K, (DateTime, DateTime)] {
    def apply(metadata: RasterMetaData, kb: KeyBounds[K], range: (DateTime, DateTime)): Option[KeyBounds[K]] = {
      val queryBounds = KeyBounds(
        kb.minKey updateTemporalComponent TemporalKey(range._1),
        kb.maxKey updateTemporalComponent TemporalKey(range._2))

      implicitly[Boundable[K]].intersect(queryBounds, kb)
    }
  }  
}