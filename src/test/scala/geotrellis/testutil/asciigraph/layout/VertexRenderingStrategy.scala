package com.github.mdr.ascii.layout

import com.github.mdr.ascii.Dimension

trait VertexRenderingStrategy[-V] {

  def getPreferredSize(v: V): Dimension

  /**
   * @return rows of text
   */
  def getText(v: V, allocatedSize: Dimension): List[String]

}