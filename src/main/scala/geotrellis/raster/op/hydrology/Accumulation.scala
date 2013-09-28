package geotrellis.raster.op.hydrology

import	geotrellis._
import scala.collection.mutable._
import	geotrellis.raster._
import scala.util.control.Breaks._


object Util{
	def calcAcc(column:Int,row:Int,data:IntArrayRasterData,raster:Raster)={
			var c = column
			var r = row
			var sum = data.get(c,r)
			val cols = data.cols
			val rows = data.rows
			var flag = 0
			println (" c=" + c + " R=" +r) 
			
			if(sum == -1){
				sum =0
				//two lists to track the coords
				var  stack= new ArrayStack[Tuple2[Int,Int]]()
				var len = 0
				stack.push(new Tuple2(c,r))
				stack.push(new Tuple2(c,r))

				while(! stack.isEmpty || data.get(c,r) == -1){
					sum = 0
					flag = 0  
				//right neighbour	
					if(c+1<cols && raster.get(c+1,r)==16){
						if(data.get(c+1,r)== -1){
							stack.push(new Tuple2(c+1,r))
							flag =1
						
						}else{
							sum= sum +data.get(c+1,r)
						}
					}
						
			//bottom right neighbor
			//
					if(c+1<cols && r+1<rows && raster.get(c+1,r+1)== 32){
						if(data.get(c+1,r+1)== -1){
							stack.push(new Tuple2(c+1,r+1))
							flag =1
						}else{
						sum = sum + data.get(c+1,r+1) +1
						}
					}
					
			//bottom neighbor
					if(r+1<rows && raster.get(c,r+1)== 64){
						if(data.get(c,r+1)== -1){
							stack.push(new Tuple2(c,r+1))
							flag =1
						}else{
							sum = sum + data.get(c,r+1) +1
						}
					}
			//bottom left neighbor
					if(c-1>=0 && r+1<rows && raster.get(c-1,r+1)== 128){
						if(data.get(c-1,r+1)== -1){
							stack.push(new Tuple2(c-1,r+1))
							flag =1
						}else{
							sum = sum + data.get(c-1,r+1)  +1
						}
					}
			//left neighbor
					if(c-1>=0 && raster.get(c-1,r)== 1){
						if(data.get(c-1,r)== -1){
							stack.push(new Tuple2(c-1,r))
							flag =1
						}
						else{
						sum = sum + data.get(c-1,r) +1
						}
					}
			//top left neighbor
					if(c-1>=0 && r-1>=0 && raster.get(c-1,r-1)== 2){
						if(data.get(c-1,r-1)== -1){
							stack.push(new Tuple2(c-1,r-1))
							flag =1
						}
						else{
						sum = sum + data.get(c-1,r-1) +1   
						}	
					}
			//top neighbor 
					if(r-1>=0 && raster.get(c,r-1)== 4){
						if(data.get(c,r-1)== -1){
							stack.push(new Tuple2(c,r-1))
							flag =1
							
						}else{
							sum = sum + data.get(c,r-1) +1   
						}
					}
						
			//top right neighbor
					if(c+1<cols && r-1>=0 && raster.get(c+1,r-1)== 8){
						if(data.get(c+1,r-1)== -1){
							stack.push(new Tuple2(c+1,r-1))
							flag =1
							
						}else{
							sum = sum + data.get(c+1,r-1) +1   
						}	
					}
				
		//set the calculated sum as the accumulation 
					if (flag == 0){ 
						data.set(c,r,sum)
					}
					if(!stack.isEmpty){
						val t = stack.pop
						c= t._1
						r= t._2
					}
				}
			}
	}
}

case class Accumulation(raster:Op[Raster]) extends Op1(raster)({raster =>


	val cols = raster.cols
	val rows = raster.rows
	val data = IntArrayRasterData(Array.ofDim[Int](cols*rows),cols,rows)
	
	var c= 0
	var r= 0
	
	while(c < cols){
		r=0
		while(r < rows){			
			data.set(c,r,-1)
			
			r=r+1
		}
		c=c+1
	}
	
	c= 0
	while(c < cols){
		r=0
		while(r < rows){
			Util.calcAcc(c,r,data,raster)
			r = r+1
		}
		c= c+1
	}
	Util.calcAcc(cols-1,rows-1,data,raster)
	//convert the IntArrayRaster to a raster
	var i=0
	var j=0
	while(i<data.rows){
				j=0
				while(j<data.cols){					
					print(""+ data.get(j,i) +",")
					j= j+1
				}
				i = i+1
				println("")
			}
	Result(Raster( data , raster.rasterExtent))
	})