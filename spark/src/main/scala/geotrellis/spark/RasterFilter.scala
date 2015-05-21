package geotrellis.spark

import geotrellis.raster.GridBounds
import com.github.nscala_time.time.Imports._
import geotrellis.vector.Extent



@annotation.implicitNotFound(msg = "Can not find RasterFilter type class to filter RasterRDD of ${K} by ${T}")
trait RasterFilter[K, T]{
  def apply(metadata: RasterMetaData, kb: KeyBounds[K], thing: T): Option[KeyBounds[K]]
}

class BoundRasterQuery[K](query: RasterQuery[K], f: RasterQuery[K] => RasterRDD[K]) {
  def filter[T](paramsList: T*)(implicit rasterFilter: RasterFilter[K, T]) = 
    new BoundRasterQuery(query.filter(paramsList:_*), f)

  def toRDD: RasterRDD[K] = f(query)
}

class RasterQuery[K: Boundable](
  filters: Tuple2[RasterMetaData, List[KeyBounds[K]]] => Tuple2[RasterMetaData, List[KeyBounds[K]]] = 
    { x: Tuple2[RasterMetaData, List[KeyBounds[K]]] => x }) {

  // Allows us to treat Function1 as an instance of a Functor
  import scalaz.Scalaz._

  def filter[T](paramsList: T*)(implicit rasterFilter: RasterFilter[K, T]): RasterQuery[K] = {
    new RasterQuery[K]( {
      filters map { case (metadata, keyBoundsList) =>
        val filteredKeyBounds = 
          for {
            keyBound <- keyBoundsList
            param <- paramsList
          } yield {
            rasterFilter(metadata, keyBound, param)
          }
        (metadata, filteredKeyBounds.flatten)
      }
    })
  }

  def apply(metadata: RasterMetaData, keyBounds: KeyBounds[K]): Seq[KeyBounds[K]] = {
    val (_, keyBoundsList) = filters((metadata, List(keyBounds)))      
    keyBoundsList
  }
}


object RasterFilter{

  implicit def gridBoundsFilter[K: SpatialComponent: Boundable] = new RasterFilter[K, GridBounds] {
    def apply(metadata: RasterMetaData, kb: KeyBounds[K], bounds: GridBounds): Option[KeyBounds[K]] = {
      val queryBounds = KeyBounds(
        kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
        kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))

      implicitly[Boundable[K]].intersect(queryBounds, kb)
    }
  }

  implicit def extentFilter[K: SpatialComponent: Boundable] = new RasterFilter[K, Extent] {
    def apply(metadata: RasterMetaData, kb: KeyBounds[K], extent: Extent): Option[KeyBounds[K]] = {      
      val bounds = metadata.mapTransform(extent)

      // TODO: This smells like copy/paste, who can I avoid this?
      val queryBounds = KeyBounds(
        kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
        kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))

      implicitly[Boundable[K]].intersect(queryBounds, kb)
    }
  }

  implicit def timeFilter[K: TemporalComponent: Boundable] = new RasterFilter[K, (DateTime, DateTime)] {
    def apply(metadata: RasterMetaData, kb: KeyBounds[K], range: (DateTime, DateTime)): Option[KeyBounds[K]] = {
      val queryBounds = KeyBounds(
        kb.minKey updateTemporalComponent TemporalKey(range._1),
        kb.maxKey updateTemporalComponent TemporalKey(range._2))

      implicitly[Boundable[K]].intersect(queryBounds, kb)
    }
  }  
}