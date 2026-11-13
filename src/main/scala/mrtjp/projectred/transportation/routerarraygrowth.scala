package mrtjp.projectred.transportation

private[transportation] object RouterArrayGrowth {
  // Match the former append loop's final capacity, including its inclusive end.
  private def capacity(index: Int): Int = {
    val size = index.toLong * 3 / 2 + 2
    require(
      index >= 0 && size <= Int.MaxValue,
      "Router index exceeds array capacity"
    )
    size.toInt
  }

  def references[A <: AnyRef](array: Array[A], index: Int): Array[A] = {
    if (array.length > index) return array
    java.util.Arrays.copyOf[A](array, capacity(index))
  }

  def versions(array: Array[Int], index: Int): Array[Int] = {
    if (array.length > index) return array
    val grown = java.util.Arrays.copyOf(array, capacity(index))
    java.util.Arrays.fill(grown, array.length, grown.length, -1)
    grown
  }
}
