package com.github.mdr.ascii.layout

import com.github.mdr.ascii.Dimension

object ToStringVertexRenderingStrategy extends VertexRenderingStrategy[Any] {

  def getPreferredSize(v: Any): Dimension = {
    val lines = splitLines(v.toString)
    Dimension(lines.size, lines.map(_.size).max)
  }

  def getText(v: Any, allocatedSize: Dimension): List[String] =
    splitLines(v.toString).take(allocatedSize.height).map { line ⇒
      val discrepancy = allocatedSize.width - line.size
      val padding = " " * (discrepancy / 2)
      padding + line
    }

  private def splitLines(s: String): List[String] =
    s.split("(\r)?\n").toList match {
      case Nil | List("") ⇒ Nil
      case xs             ⇒ xs
    }

}