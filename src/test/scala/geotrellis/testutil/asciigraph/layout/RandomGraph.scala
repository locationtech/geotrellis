package com.github.mdr.ascii.layout

import scala.util.Random

object RandomGraph {

  def randomGraph(implicit random: Random): Graph[String] = {

    def mkVertex: String = {
      val chars = "abcdef\n"
      def mkChar = chars(random.nextInt(chars.length))
      (1 to (random.nextInt(20) + 1)).toList.map(_ ⇒ mkChar).mkString
    }

    val numberOfVertices = random.nextInt(10) + 1

    val vertices = (1 to numberOfVertices).toList.map(_ ⇒ mkVertex)

    val edges =
      if (vertices.isEmpty)
        Nil
      else {
        val numberOfEdges = random.nextInt(numberOfVertices * 2)

        def mkEdge: (String, String) = {
          val i1 = random.nextInt(numberOfVertices)
          var i2 = random.nextInt(numberOfVertices)
          if (i2 == i1)
            i2 = (i1 + 1) % numberOfVertices
          val v1 = vertices(i1)
          val v2 = vertices(i2)

          (v1, v2)
        }

        (1 to numberOfVertices).toList.map(_ ⇒ mkEdge)

      }

    Graph(vertices, edges)

  }

}