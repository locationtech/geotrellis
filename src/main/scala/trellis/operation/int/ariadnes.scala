package trellis.operation

import trellis.raster._
import collection.mutable.PriorityQueue

/// 
/// Azavea Research Labs Cost-Distance algorithm, affectionately known
/// as "The Ariadnes Algorithm".
/// 
/// based on dzwarg's c# port & Tomlin's Python 

case class Popper(pV:Int, pH:Int, neighborV:Int, direction:Int)

/// <summary>
/// A static class that computes a cost-distance array.  This class has only one
/// method, and that is the spreading algorithm.
/// </summary>
class Spreader {
  val debug = false

  def logf(fmt:String, stuff:Any*) = if (debug) printf(fmt, stuff:_*)

  /// The Heart of Ariadnes, in the form of the Spread function. This method takes
  /// a set of rasters, and computes the cost-distance to the target raster. All raster 
  /// layers must have the same dimensions.

  /// <param name="targetArray">The target array. Target cells have values in them,
  /// and the cost-distance will be computed to the cells that have no values.</param>
  /// <param name="frictionArray">The friction array. 0 and no data are treated the same.</param>
  /// <param name="elevationArray">The elevation array. To neglect elevation, use an 
  /// array full of 1's..</param>
  /// <param name="width">The width of the input rasters.</param>
  /// <param name="height">The height of the input rasters.</param>
  /// <param name="distance">The distance to compute until. If the distance exceeds the
  /// cost to the farthest pixel, the algorithm will compute the cost to all pixels. If the
  /// distance is less than the cost to the farthest pixel, the algorithm will compute all
  /// costs less than the distance, and quit early.</param>
  /// <param name="cellWidth">The width of a single cell in the rasters, in geographical units.</param>
  /// <param name="cancelled">A delegate that can be called to determine if the algorithm
  /// should cancel processing.</param>
  /// <returns></returns>

  /* all float[][] become single dimensional arrays */
    def Spread( height:Int, width:Int, target:DoubleRaster, friction:DoubleRaster, 
      elevation:DoubleRaster, maxDistance:Int, cellWidth:Double): DoubleRaster = {

      logf(" * Starting Spread() function.\n")
      // Initialize values
      val size = width * height

      val output   = Array.ofDim[Double](width * height)
      val vMatrix  = Array.ofDim[Double](width * height)
      val hMatrix  = Array.ofDim[Double](width * height)

      val resolution:Double = cellWidth   // Grid cell width in geographical units
      val elevationAmplification:Int = 1 // Amplify elevation by this much
      var run:Double = 0.0                // Number of geographical units between centers of orthogonally or diagonally adjacent cells
      //var frictionConstant:Double = 0.0           // Travelcost distance centers of orthogonally or diagonally adjacent cells
      //var popperFriction:Double = 1.0
      var AddedCost:Double = 1.0;         // Slope-weighted travelcost distance centers of orthogonally or diagonally adjacent cells
      var Reach:Double = 0.0              // Inclined travelcost distance centers of orthogonally or diagonally adjacent cells
      var NeighborV:Double = 0.0          // Vertical component of a neighbor's incoming angle
      var NeighborH:Double = 0.0          // Horizontal component of a neighbor's incoming angle

      // The c# refers to NaN -- we'll use 'NODATA' value
      var NO_DATA = -1

      // python section: INITIALIZE SOME ARRAYS AND LOAD THE POPPER LIST WITH TARGET CELLS

      // Vertical offsets for a popper's neighbors
      val RowShift = Array(-1, 0, 1, 0, -1, -1, 1, 1)

      // Horizontal offsets for a popper's neighbors
      val ColumnShift = Array(0, 1, 0, -1, -1, 1, 1, -1)

      val CompassCode = Array(32, 64, 128, 16, 0, 1, 8, 4, 2)
      val PopperCount = 0
    
      // a "popper" is a tuple of the form (distance,x,y)
      // which indicates work to be done -- a cell that must be processed.

      // we store the cells in a priority queue because we want to keep them
      // sorted so that we can handle low distance cells before high distance cells

      // sort by first integer in integer 3-tuple, lowest first
      def PopperOrdering = new Ordering[(Double,Int,Int)] { 
        def compare(a: (Double,Int,Int), b: (Double,Int,Int)) = - Ordering[Double].compare(a._1,b._1)  

      }
      val poppers = new PriorityQueue[(Double,Int,Int)]()(PopperOrdering)

      logf( " * About to loop through initial rasters for initialization.\n")
      for (y <- 0 until height) {
        for (x <- 0 until width) {
          logf(" * * Checking %d,%d for target cell\n", x, y)
          if (friction.get(x, y) < 1) friction.set(x, y, 1)
          val initialDistance = target.get(x,y)
          // if we find a cell with a value in the 'target' raster, it's a starting cell.
          // it's value is its 'headstart' (initial distance value)
          if (initialDistance >= 0) { // target cell w/ 'headstart' (initial distance value)
            poppers += ((initialDistance, y, x))
            output(y * width + x) = initialDistance
            logf(" * * Found target cell at %d,%d\n", x, y)
          } else {
            output(y * width + x) = maxDistance
          }
        }
      }

      // python comment: LOOP THROUGH THE POPPER LIST
      var p = poppers.dequeue 
      // pcost is accumulated travelcost distance of this "popper"
      // px & py are its coordinates

      // stop when accumulated travelcost is > maxDistance
      var pcost = 0.0
      var py = 0
      var px = 0
      logf(" * About to start popper loop.  First popper: %s\n", p.toString)

      while (pcost < maxDistance && p != null ) { 
        logf(" * * In popper loop, popper: %s\n", p.toString)
        val (pcost, py, px) = p
        val pindex = py * width + px

        logf(" * * Coordinate: %d, %d", px, py)

        // Incremental travelcost friction of the current popper
        val popperFriction = friction.get(px,py) // PopperFriction in python 

        // Vertical & Horizontal components of the popper's incoming angle
        val pV = vMatrix(pindex)
        val pH = hMatrix(pindex)
      
        logf(" * * Vertical   component of incoming angle: %f\n", pV)
        logf(" * * Horizontal component of incoming angle: %f\n", pH)

        // python comment: LOOP THROUGH A GIVEN POPPER'S NEIGHBORS

        // Establish popper's neighborhood without going beyond the edge
        val top    = math.max(py-1, 0) 
        val bottom = math.min(py+1, height-1) 
        val left   = math.max(px-1, 0)
        val right  = math.min(px+1, width-1)

        for (y <- top until bottom + 1  ) {
          val neighborV = py - y
          for (x <- left until right + 1) {
            logf(" * * * Looking at neighbor (%d,%d)\n", x, y)
            if (! ( x == px && y == py )) { // skip neighborhood center
              logf(" * * * Neigbor is not neighborhood center (popper)\n")
              var index = y * width + x
              val neighborH = x - px
              
              val adjacent = (y == py || x == px) 
              val horizontal = if (adjacent) 1.0 else math.sqrt(2) 
              val run = resolution * horizontal
              val frictionDivisor = if (adjacent) 2 else math.sqrt(2)
              val travelFriction = (popperFriction + friction.get(x,y)) / frictionDivisor // travelFriction is Friction in python


              // python: USE THE 3D SURFACE TO ACHIEVE PLUME-LIKE DISSIPATION OF DISTANCE VALUES
              //         IN AN MANNER THAT I EXPECT TO REFINE FURTHER BUT WILL LEAVE AS IS FOR NOW

              val rise = (elevation.get(x,y) - elevation.get(px,py)) * elevationAmplification 
              val tangent = rise / run
              var reach = run + (run * tangent)
              val radians = math.atan2(rise,run)
              reach = reach / math.cos(radians)
              val addedCost = travelFriction * (reach / run)

              // python:--  CALCULATE NEIGHBOR'S N-NE-E-SE-S-SW-W-NW BEARING
              val direction = CompassCode((3 * (y + 1 - py)) + x + 1 - px)

              // python: CALCULATE NEIGHBOR'S ACCUMULATED TRAVEL COST DISTANCE WITH ARIADNE COMPENSATION

              var distance = pcost + addedCost

              var turnWeight =(math.sqrt(math.pow((pV+neighborV),2)+math.pow((pH+neighborH),2)))-(math.sqrt(math.pow(pV,2)+math.pow(pH,2)))

              turnWeight = turnWeight / math.sqrt(math.pow(neighborV,2)+math.pow(neighborH,2)) 

              turnWeight = if (turnWeight < 0.82) 1.0 else turnWeight
              distance = if (pcost != 0.0) pcost + (addedCost + turnWeight) else distance

              logf(" * * Is neighbor's calculated distance more than the existing one?\n")
              logf(" * * * distance: %f\n", distance)
              logf(" * * * exiting output(index): %f\n", output(index))

              // if neighbor's calculated distance does not exceed an earlier one ...
              if (distance < output(index)) {
                logf(" * * * No, calculating new distance to neighbor.\n")

                // REMEMBER THE HORIZONTAL AND VERTICAL COMPONENTS OF THE NEIGHBOR'S INCOMING ANGLE
                vMatrix(index) = pV + neighborV
                hMatrix(index) = pH + neighborH 

                // IDENTIFY THE PIVOT CELL ASSOCIATED WITH THIS NEIGHBOR

                //pivotY = py
                //pivotX = px 
                
                var (pivotX, pivotY) = (Ordering[Double].compare(pV,0), Ordering[Double].compare(pH,0), neighborH, direction) match {
                  // Heading Northward then turning
                  case ( 1,  0, -1, _) => (px-1,py) // Westerly, pivot west of popper
                  case ( 1,  0,  1, _) => (px+1,py) // Easterly, pivot east
                  // Heading Southward then turning
                  case (-1,  0,  1, _) => (px+1,py) // Easterly, pivot east
                  case (-1,  0, -1, _) => (px-1,py) // Westerly, pivot west
                  // Heading Eastward then turning
                  case ( 0,  1,  1, _) => (px, py+1) // Northerly, pivot north
                  case ( 0,  1, -1, _) => (px, py-1) // Southerly, pivot south
                  // Heading Westerward then turning
                  case ( 0, -1, -1, _) => (px, py+1) // Southerly, pivot south
                  case ( 0, -1,  1, _) => (px, py-1) // Northerly, pivot north
                  // Heading Northeasterly then turning
                  case ( 1,  1,  _, 64) => (px-1, py) // North, pivot west
                  case ( 1,  1,  _,  1) => (px, py+1) // East, pivot south
                  case ( 1,  1,  _,  128) if pV/pH < 1 => (px, py-1) // left to Northeast, pivot north
                  case ( 1,  1,  _,  128) if pV/pH > 1 => (px+1, py) // right to Northeast, pivot east 
                  // Heading Southeasterly then turning
                  case (-1,  1,  _,  1) => (px, py-1) // East, pivot north
                  case (-1,  1,  _,  4) => (px-1, py) // South, pivot west 
                  case (-1,  1,  _,  2) if pV/pH < -1 => (px+1, py) // left to southeast, pivot east
                  case (-1,  1,  _,  2) if pV/pH > 1 => (px, py+1) // right to southeast, pivot south
                  // Heading Southwesterly then turning
                  case (-1, -1,  _,  4) => (px+1, py) // South, pivot east
                  case (-1, -1,  _,  16) => (px, py-1) // West, pivot north 
                  case (-1, -1,  _,  8) if pV/pH < -1 => (px, py+1) // left to southwest, pivot south
                  case (-1, -1,  _,  8) if pV/pH > 1 => (px-1, py) // right to southwest, pivot west
                  // Heading Southwesterly then turning
                  case ( 1, -1,  _,  16) => (px, py+1) // West, pivot south
                  case ( 1, -1,  _,  64) => (px+1, py) // North, pivot east
                  case ( 1, -1,  _,  32) if pV/pH < -1 => (px-1, py) // left to northwest, pivot west
                  case ( 1, -1,  _,  32) if pV/pH > 1 => (px, py-1) // right to northwest, pivot north
                  case _ => (px, py)
                }


                //  IF PIVOTING, CANCEL THE ARIADNE EFFECT
                if (friction.get(pivotX,pivotY) > popperFriction || friction.get(x,y) != friction.get(px,py) ) {
                  logf("pivoting, cancelling ariadne effect.\n")
                  vMatrix(index) = neighborV
                  hMatrix(index) = neighborH
                  distance       = pcost + addedCost
                } else {
                  logf("not pivoting\n")
                }

                
                logf("distance: %f\n", distance)
                logf("output(index): %f\n", output(index))
                if (distance != output(index)) {
                  output(index) = distance

                  //add this neighbor to popper list
                  poppers += ((distance, y, x))
                }
              } else {
                logf(" * * * This isn't a neighbor -- it's the popper coordinate.\n")
              } 
            }  
          }
        }
        logf(" * * finished with popper %s\n", p.toString())
        if (poppers.nonEmpty) {
         
          p = poppers.dequeue
        } else {
          logf(" * no more poppers.\n")
          p = null
        }
      }            
      val outputRaster = new DoubleRaster( output, target.rows, target.cols,
                                           target.rasterExtent )
      logf(outputRaster.asciiDraw)
      outputRaster
    }
  }
