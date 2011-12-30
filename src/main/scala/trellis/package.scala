package trellis

package object process {
  type Callback[T] = (List[Any]) => StepOutput[T]
  type Args = List[Any]

  def time() = System.currentTimeMillis
}
