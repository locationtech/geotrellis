package geotrellis.engine

package object actors {
  type Callback[+T] = (List[Any]) => StepOutput[T]
}
