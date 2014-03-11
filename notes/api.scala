/**************************************************************************
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

// Getting min of raster
val r = io.LoadRaster("")  // RasterSource extends DataSource[Raster,Raster]
val hist:DataSource[Histogram,Histogram] = r.histogram
val ints:SeqDataSource[Int] = hist.chunks.map(_.min)
val result = ints.reduce(math.min(_,_))

// value here means -- a single value.

io.LoadRaster("")
  .histogram:DataSource[Histogram,Histogram]
  .map(_.getMin):DataSource[Int,Seq[Int]]
  .reduce(math.min(_,_)):DataSource[Int,Int] //ValueDS[Int]
  .run(server)

io.LoadRaster("")
  .histogram:HistogramDataSource extends DataSource[Histogram,Histogram] 
   .map(_.getMin):DataSource[Int]
   .reduce(math.min(_,_)):ValueSource[Int]
   .map(_ + 1) : ValueSource[Int]
   .run(server)

trait DataSource[T] {
  def map(f:T=>B)
  def chunks:Op[Seq[T]]
}

RasterSource extends DataSource[Raster] {
  def localAdd(r:RasterSource)
  def 
}

DataSource[T]
ConvergedDataSource[T] { 
  def get:T = { chunks(0) }
}

val histogram = io.LoadRaster("bar").histogram // DataSource[Histogram]

// HistogramDataSource
trait ConvergableInto[T] {
  def get:Op[T] = ???
}



val r:RasterSource


rOp.map(r => r.map( _ + 1 ) )

io.LoadRaster("")
  .histogram:HistogramDataSource
  .map(_.getHugeThing):SeqDS[HugeThing]
  .map(_.toInt):SeqDS[Int]
  .map(io.LoadTile("asdf",_))
  .map(_.histogram)
  .map(_.min)
  .reduce(math.min(_+_)):LocalValueDS[Int]
  .run(server)

io.LoadRaster("")
  .histogram:ChunkedValueDS[Histogram,Histogram]
  .into(_.getMin):ValueDS[Int]
  .into(0 until i):LocalSeqDS[Int]
  .toDistributed:DistributedSeqDS[Int]
  .map(somethingComplicated(_)):DistributedSeqDS[Int]

io.LoadRaster("")
  .histogram:ChunkedValueDS[Histogram,Histogram] with Distributed
//  .distribute
  .into(_.getMin):ValueDS[Int] with Local  // T => A
//  .distribute!:DistributedValueDS[Int]
  .into(0 until _):LocalSeqDS[Int]    // T => A  .. if A is Seq[B] get LocalSeqDS
//  .distribute!
  .map(somethingComplicated(_)):LocalSeqDS[Int]
  .distribute!


// Other
val ro = io.LoadRaster("other")

io.LoadRaster("")
  .histogram:ChunkedValueDS[Histogram,Histogram]
  .into(_.getMin):ValueDS[Int]
  .into(i => ro.localAdd(i))

// ValueDS[T]  T is one thing, unchunked <- Local
// 

io.LoadRaster(""):RasterDS
  .tiles:TileSetDS //<: TiledSeqDS[Raster]  (get toRaster)
  .filter(_.rasterExtent.extent.containPoint(x,y)):TiledSeqDS[Raster]
  .map(_.histogram):SeqDS[Histogram]  //.map(GetHistogram(_))
  .map(_.getMin):SeqDS[Int]
  .reduce(math.min(_,_)):ValueDS[Int]
  .run(server):Int

// Add with Raster
val r:RasterSource = io.LoadRaster("1")
val ar:RasterSource = r.localAdd(5)
val tiles:TileDataSource = ar.tiles

// Add with tiles
val atiles:TileDataSource = r.tiles.map(local.Add(_,5))
val atiles:TileDataSource = r.tiles.localAdd(5)
val atr:RasterSource = atiles.toRaster

tiles === atiles
atr === ar

// Vectorization
val r = io.LoadRaster("1")
val rgrs:SeqDataSource[RegionGroupResult] = r.tiles.map(RegionGroup(_))
val vectors:SeqDataSource[Seq[(Extent,Polygon)]] = 
  rgrs.chunks.map({ rgr => (rgr.raster.rasterExtent.extent, ToVector(rgr)))
val result:ValueSource[MultiPolygon] = 
  vectors.into { seq =>
    //returns MultiPolygon
  }

// Vectorization 2
val r = io.LoadRaster("1")
val rgrs:TileDataSource[RegionGroupResult] = r.tiles.map(RegionGroup(_))
val vectors:SeqDataSource[Seq[(Extent,Polygon)]] =
  rgrs.mapWithExtent { rgr => ToVector(rgr) }
val result:ValueSource[MultiPolygon] = 
  vectors.into { seq =>
    //returns MultiPolygon
  }

// Polygons
val polys:PolygonDataSource = io.LoadPolygons("stuff")
val datas:BoundingBoxSeqSource = polys.items.map(_.data)
val min:ValueSource[Int] = datas.reduce(math.min(_,_))
min.run(server)

// Weighted Overlay
val r1:DistRasterDS = io.LoadRaster("one")
val r2:DistRasterDS = io.LoadRaster("two")

val w1:DistRasterDS = r1.localMultiply(3)
val w2:DistRasterDS = r2.localMultiply(2)

val r:DistRasterDS = w1.localAdd(w2)

val result:Raster = r.run(server)


w1.combine(w2)(local.Add(_,_))

val tiles = w1.tiles.zip(w2.tiles).map { case (t1,t2) => local.Add(t1,t2) }
CorrectRasterDS(tiles)

w1:DistRasterDS
w2:LocalRasterDS

w1.tiles:DistTileSetDS
  .combine(w2.tiles:LocalTileSetDS) { (t1,t2) => local.Add(t1,t2) }:LocalTileSetDS[Raster]
  .toRaster:LocalRasterDS

w1.tiles:DistTileSetDS
  .zip(w2.tiles:LocalTileSetDS):LocalTiledSeqDS[(Raster,Raster)]
  .map { case (t1, t2) => local.Add(t1,t2) }:LocalTileSetDS[Raster]
  .toRaster:LocalRasterDS

w1.combine(w2) { (t1,t2) => local.Add(t1,t2) }

w1.combine(w2) { (t1,t2) => 
  t1.combine(t2) { math.min(_,_) }
}

w1.tiles.map { t1 => local.Add(t1,2) }
w1.tiles.map! { r => r.mapIfSet( _ + 2 ) }
//w1.tiles.map# { z => z + 2 }

w1.tiles.flatMap { r => local.Add(r.mapIfSet( _ + 2),2) }

(w1:DistRasterDS).map { tile:Op[Raster] => local.Add(tile,2) }

(w1:DistRasterDS).into { r:Raster => r.mapIfSet( _ + 2) }


// Focal ops

r1.focal(Square(2)) { (r,n) => focal.Min(r,n) }


r1.focalMin(Square(2))

def focalMin(n:Neighborhood) = this.focal(n)(focal.Min(_,_))


def focal(n:Neighborhood)((Op[Raster],Neighborhood)=>Op[Raster]) = {
  
}
