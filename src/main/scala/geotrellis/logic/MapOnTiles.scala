package geotrellis.logic

import scala.math.{ min, max }

import geotrellis._
import geotrellis.raster.op.tiles.GetTileOps



object Map {  
//  def apply[A:Manifest,R](r: Op[DataSource[R]])(f: Op[R] => Op[A]): Op[DataSource[A]] = {
  def apply[A:Manifest,R](r: Op[DataSource[R]])(f: Op[R] => Op[A]): Op[Seq[Op[A]]] = {
    val partitions = r.flatMap {
      _.partitions
    }
   // Literal(LiteralSource(Map.apply2(partitions)(f)))
    Map.apply(partitions)(f)
  }
  
  def apply[A:Manifest,R](partitions: Op[Seq[Op[R]]])(f:Op[R] => Op[A])(implicit di:DummyImplicit):Op[Seq[Op[A]]] = {
    val r = partitions.flatMap( _.map(f(_)))
    
    r
  }
}


/*
object MapOverTiles {
  def apply[A: Manifest](r: Op[Raster])(f: Op[Raster] => Op[A])(implicit di:DummyImplicit): Op[Seq[A]] = {
    val tiles = GetTileOps(r)
    MapOverTiles(tiles)(f)
  }
  
  def apply[A:Manifest](tiles:Op[Seq[Op[Raster]]])(f:Op[Raster] => Op[A]): Op[Seq[A]] = {
    tiles.flatMap(seq => {
      val results = seq.map(opR =>
        f(opR))
      geotrellis.logic.Collect(results)
    })
  }
}
*/
/*
object Foo {
  def bar = r.tiles.filterByPolygon(p).mapOverTiles(mapper).map 
  
}
* 
*/
//
/*object MapOverTiles2 {
  def apply[A: Manifest](r: Op[Raster])(f: Op[Raster] => Op[A]): Op[Seq[A]] = {
    val tiles = GetTileOps(r)
    tiles.flatMap(seq => {
      val results = seq.map(opR =>
        f(opR))
      geotrellis.logic.Collect(results)
    })
  }
}*/



/*
object Min {
  def mapper(r:Op[Raster]) = UntiledMin(r)
  def reducer(result:Op[Seq[Int]]) = result.map ( _.reduceLeft(min))
    
  def apply(r:Op[Raster]) = reducer(MapOverTiles(r)(mapper))
 // def apply(tiles:Op[Seq[Op[Raster]]]) = reducer(MapOverTiles(tiles)(mapper))
  
//  def foo = r.tiles.filterByPolygon.mapByTiles()
  
  case class UntiledMin(r:Op[Raster]) extends Op1(r) ({
    (r) => {
      var zmin = Int.MaxValue
      r.foreach(z => if (z != NODATA) zmin = min(z, zmin))
      Result(zmin)
    } 
  })

}
*/

//object ReduceOverTiles {
//  def apply[A](ops:Op[Seq[A]])
//  
//}

/*
  def mapOnTiles[A:Manifest](r:Op[Raster])(f:Op[Raster] => Op[A]):Op[Seq[Op[A]]] = {	  
	  val mappedOps:Op[Seq[Op[A]]] = GetTileOps(r).flatMap {
	    seq => seq.map { opR =>
	      f(opR)
	    }
	  }
	  
	  Collect(mappedOps) 
  }*/
//case class MapOnTiles[A](r:Op[Raster])(f:Op[Raster] => Op[A]) extends Op1(r) ({
//  Result(???)  
//})
/*
case class Min(r:Op[Raster]) extends logic.TileReducer1[Int] {
  type B = Int


  def mapper(r: Op[Raster]) = logic.AsList(UntiledMin(r))
  def reducer(mapResults:List[Int]) = mapResults.reduceLeft(min)

}

object Min {
  case class UntiledMin(r:Op[Raster]) extends Op1(r) ({
    (r) => {
      var zmin = Int.MaxValue
      r.foreach(z => if (z != NODATA) zmin = min(z, zmin))
      Result(zmin)
    } 
  })

  def mapper(r: Op[Raster]) = logic.AsList(UntiledMin(r))
  def reducer(mapResults:List[Int]) = mapResults.reduceLeft(min)

}

object MapOnTiles {

 //def apply(r:Op[Raster]) = r.mapOnTiles(mapper).reduce(reducer)
// r.mapOnTiles(mapper).distribute(cluster)
 
  def x(r:Op[Raster]) = {
	  .flatmapOnTiles { r =>
	     var zmin = Int.MaxValue
	     r.foreach(z => if (z != NODATA) zmin = min(z, zmin))
	     zmin
	  } reduce { results =>
	    results.reduceLeft(min)
	  }
	  
	  //[R => Int]
	  
	  //R => Op[Int]
	  
	  //Op[R] => Op[Int]
	  
  }
  
 object Collect {
   def apply[T](op:Op[Seq[Op[T]]]):Op[Seq[T]] = ???
 }

  Seq[A] -> Seq[B]
  Seq[B] -> C
 seq.map(mapper).reduce(reducer)  
 
 List[Op[Int]] -> List[B]
 
 case class MapOnTiles[A:Manifest](r:Raster) extends Op[Seq[A]] {
   
   def distribute(cluster) = DistributedMapOnTiles(r,cluster)
 }
 
 var seq:Op[Seq[Op[Int]]] = Op[Seq[A]]
 
// val mapOp = MapOnTiles(GetTileOps(r))(mapper)
    
 seq.mapOnTiles(mapper).reduce(reducer)
 
 
 //reducer(op[Seq[A]])
 
 //seq.map.sort.reduce(reducer)
 
 case class Reducer[A](ops:Op[Seq[Op[A]]]) {
   def run = runAsync(List(ops))
   def stepTwo(seq:Seq[Op[A]]) = {
     //... collect step
   }
   def stepThree() = {
     // actually reduce
   }
   
 }
  
 def seqMapOnIntList[A:Manifest](seq:List[Op[Int]])(f:Op[Int] => A) = {
   seq.map(f)
 }
 
 
 
  def mapOnIntList[A:Manifest](seq:Op[List[Op[Int]]])(f:Op[Op[Int] => Op[A]]) = {
      val foo = r.flatMap(_.map(f))
      val result = Collect(foo)
      result
  }  
 
  def mapOnTiles[A:Manifest](r:Op[Raster])(f:Op[Raster] => Op[A]):Op[Seq[Op[A]]] = {	  
	  val mappedOps:Op[Seq[Op[A]]] = GetTileOps(r).flatMap {
	    seq => seq.map { opR =>
	      f(opR)
	    }
	  }
	  
	  Collect(mappedOps) 
  }
  
  
 
 // mapOnTiles.
  
//  Collect(op:Op[Seq[Op[A]]]):Op[Seq[A]]
  
	//  Seq[Op[R]]
	   
	  
	//  r.mapOnTiles(Add(_, 4))
	//  Op[R] => Op[Int]
	//  R => Int
	//  R => Op[Int]
	  
	  
  //s}
  val mapperOp = MapOnTiles(r)(mapper)
  val collectedOp = Collect(mapperOp).dispatch(cluster).executeInGroups(30)
  val result = Reducer(collectedOp)
  
  
  
  //val dispatched:Op[Seq[Op[A]]] = DispatchedOperation(mapperOp, cluster)
  
  
  ///def apply(r:Op[Raster]) = Reducer((reducer)

}

Op[Int]
 -- Result(4) : StepResult[T]

Int
StepResult[Int]
Op[Int]


function[T,U]

.?[T => StepResult[U]] 
.?[T => StepResult[U], nextFunction]

.flatMap[T => Op[U]]
.map[T => U]


*/
