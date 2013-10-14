package geotrellis.raster.op.hydrology

import	geotrellis._
import scala.collection.mutable._
import	geotrellis.raster._
import scala.util.control.Breaks._


object util{
	def calcAcc(column:Int,row:Int,data:IntArrayRasterData,flowDirrection:Raster)={
			var c = column
			var r = row
			var sum = data.get(c,r)
			val cols = data.cols
			val rows = data.rows
			var flag = 0

			if(sum == -1){
				sum =0
				//two lists to track the coords
				var  stack= new ArrayStack[(Int,Int)]()
				var len = 0
				stack.push((c,r))
				stack.push((c,r))

				while(! stack.isEmpty || data.get(c,r) == -1){
					sum = 0
					flag = 0  
				//right neighbour	
					if(c+1<cols && flowDirrection.get(c+1,r)==16){
						if(data.get(c+1,r)== -1){
							stack.push((c+1,r))
							flag =1

						}else{
							sum= sum +data.get(c+1,r)
						}
					}

			//bottom right neighbor
			//
					if(c+1<cols && r+1<rows && flowDirrection.get(c+1,r+1)== 32){
						if(data.get(c+1,r+1)== -1){
							stack.push((c+1,r+1))
							flag =1
						}else{
						sum = sum + data.get(c+1,r+1) +1
						}
					}

			//bottom neighbor
					if(r+1<rows && flowDirrection.get(c,r+1)== 64){
						if(data.get(c,r+1)== -1){
							stack.push((c,r+1))
							flag =1
						}else{
							sum = sum + data.get(c,r+1) +1
						}
					}
			//bottom left neighbor
					if(c-1>=0 && r+1<rows && flowDirrection.get(c-1,r+1)== 128){
						if(data.get(c-1,r+1)== -1){
							stack.push((c-1,r+1))
							flag =1
						}else{
							sum = sum + data.get(c-1,r+1)  +1
						}
					}
			//left neighbor
					if(c-1>=0 && flowDirrection.get(c-1,r)== 1){
						if(data.get(c-1,r)== -1){
							stack.push((c-1,r))
							flag =1
						}
						else{
						sum = sum + data.get(c-1,r) +1
						}
					}
			//top left neighbor
					if(c-1>=0 && r-1>=0 && flowDirrection.get(c-1,r-1)== 2){
						if(data.get(c-1,r-1)== -1){
							stack.push((c-1,r-1))
							flag =1
						}
						else{
						sum = sum + data.get(c-1,r-1) +1   
						}	
					}
			//top neighbor 
					if(r-1>=0 && flowDirrection.get(c,r-1)== 4){
						if(data.get(c,r-1)== -1){
							stack.push((c,r-1))
							flag =1

						}else{
							sum = sum + data.get(c,r-1) +1   
						}
					}

			//top right neighbor
					if(c+1<cols && r-1>=0 && flowDirrection.get(c+1,r-1)== 8){
						if(data.get(c+1,r-1)== -1){
							stack.push((c+1,r-1))
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

case class Accumulation(flowDirrection:Op[Raster]) extends Op1(flowDirrection)({flowDirrection =>


	val cols = flowDirrection.cols
	val rows = flowDirrection.rows
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
			util.calcAcc(c,r,data,flowDirrection)
			r = r+1
		}
		c= c+1
	}

	//convert the IntArrayflowDirrection to a flowDirrection

	Result(Raster( data , flowDirrection.rasterExtent))
	})