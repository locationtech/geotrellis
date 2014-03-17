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

package geotrellis.logic

import geotrellis._
import geotrellis.process._

object Filter {
  def apply[A](ops:Op[Seq[A]], condition:(A) => Boolean) = ops.map(_.filter(condition))

  def apply[A:Manifest](opsOp:Op[Seq[A]], condition:(A) => Op[Boolean]):Op[Seq[A]] = 
    opsOp.flatMap (
      (seq) => {
        seq.foldLeft (Literal(Seq[A]()):Operation[Seq[A]]) { 
          (sum:Op[Seq[A]],v:A) =>
            for( s <- sum;
                 bool <- condition(v) ) yield {
              if (bool) s :+ v else s
            }
        }
      })
}

