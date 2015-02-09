package geotrellis.vector.io.shape.reader

import geotrellis.vector._
import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.algorithm.CGAlgorithms
import spire.syntax.cfor._

import scala.collection.mutable
import java.nio.ByteBuffer

import GeomFactory._

class MultiPolygonReader(val dimensions: Int) extends GeometryReader(dimensions) {
  def readGeometry(byteBuffer: ByteBuffer): Geometry = {
    val lineCount = byteBuffer.getInt
    val pointCount = byteBuffer.getInt

    val lines = Array.ofDim[Line](lineCount)
    val lineLengths = Array.ofDim[Int](lineCount)

    var startPoint = byteBuffer.getInt // First offset
    cfor(0)(_ < lineCount, _ + 1) { lineIndex =>
      val endPoint = 
        if(lineIndex == lineCount - 1) pointCount 
        else byteBuffer.getInt

      lineLengths(lineIndex) = endPoint - startPoint
      startPoint = endPoint
    }

    val shells = mutable.ListBuffer[jts.LinearRing]()
    val holes = mutable.ListBuffer[jts.LinearRing]()
    cfor(0)(_ < lineCount, _ + 1) { lineIndex =>
      val len = lineLengths(lineIndex)
      val coords = Array.ofDim[jts.Coordinate](len)
      cfor(0)(_ < len, _ + 1) { pointIndex =>
        val x = byteBuffer.getDouble
        val y = byteBuffer.getDouble
        coords(pointIndex) = new jts.Coordinate(x, y)
      }

      val ringCoords = 
        if(coords(0) == coords(len-1)) {
          coords
        } else {
          val closedCoords = Array.ofDim[jts.Coordinate](len + 1)
          cfor(0)(_ < len, _ + 1) { i =>
            closedCoords(i) = coords(i)
          }
          closedCoords(len) = coords(0)
          closedCoords
        }

      if(CGAlgorithms.isCCW(ringCoords))
        holes += factory.createLinearRing(ringCoords)
      else
        shells += factory.createLinearRing(ringCoords)
    }

    val shellArray = shells.toArray
    val holeArray = holes.toArray

    if(shells.size == 1) {
      MultiPolygon(
        factory.createMultiPolygon(
          Array(
            factory.createPolygon(
              shellArray(0),
              holeArray
            )
          )
        )
      )
    } else if(holeArray.size == 0) {
      val polyArray = Array.ofDim[jts.Polygon](shells.size)
      val len = shells.size
      cfor(0)(_ < len, _ + 1) { i =>
        polyArray(i) = factory.createPolygon(shellArray(i), null)
      }
      MultiPolygon(factory.createMultiPolygon(polyArray))
    } else {
      // Determine what holes go to what shells
      val holesLen = holeArray.size
      val shellsLen = shells.size

      val shellEnvelopes = Array.ofDim[jts.Envelope](shellsLen)
      val shellHoles = Array.ofDim[mutable.ListBuffer[jts.LinearRing]](shellsLen)
      val badHoles = mutable.ListBuffer[jts.LinearRing]()

      cfor(0)(_ < holesLen, _ + 1) { holeIndex =>
        val hole = holeArray(holeIndex)
        val holeEnv = hole.getEnvelopeInternal
        val holeCoord = hole.getCoordinateN(0)

        var owningShellIndex = -1

        cfor(0)(_ < shellsLen, _ + 1) { shellIndex =>
          val shell = shellArray(shellIndex)
          val shellEnv = {
            val env = shellEnvelopes(shellIndex)
            if(env == null) {
              val computedEnv = shell.getEnvelopeInternal
              shellEnvelopes(shellIndex) = computedEnv
              computedEnv
            } else {
              env
            }
          }

          // Test this shell
          if(shellEnv.contains(holeEnv) && CGAlgorithms.isPointInRing(holeCoord, shell.getCoordinates)) {
            if(owningShellIndex == -1) {
              owningShellIndex = shellIndex
            } else {
              // If the envlope (computed in a previos iteration) contains this shell envelope,
              // then take the smaller shell.
              if(shellEnvelopes(owningShellIndex).contains(shellEnv)) {
                owningShellIndex = shellIndex
              }
            }
          }
        }

        if(owningShellIndex == -1) {
          badHoles += hole
        } else {
          if(shellHoles(owningShellIndex) == null) {
            shellHoles(owningShellIndex) = mutable.ListBuffer[jts.LinearRing]()
          }
          shellHoles(owningShellIndex) += hole
        }
      }

      val badHoleArray = badHoles.toArray
      val polygons = Array.ofDim[jts.Polygon](shellsLen + badHoleArray.size)
      cfor(0)(_ < shellsLen, _ + 1) { i =>
        val theseHolesArray: Array[jts.LinearRing] = {
          val theseHoles = shellHoles(i)
          if(theseHoles == null) null
          else theseHoles.toArray
        }

        polygons(i) = 
          factory.createPolygon(
            shellArray(i),
            theseHolesArray
          )
      }

      cfor(shellsLen)(_ < polygons.size, _ + 1) { i =>
        polygons(i) =
          factory.createPolygon(
            badHoleArray(i),
            null
          )
      }
              
      MultiPolygon(factory.createMultiPolygon(polygons))
    }
  }
}
