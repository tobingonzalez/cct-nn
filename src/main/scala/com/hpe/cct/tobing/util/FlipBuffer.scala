package com.hpe.cct.tobing.util

import scala.reflect.ClassTag

/**
 * A pair of arrays - one of which is the target up `update`s, the other is
 * the target of gets/`apply`s. The roles can be reversed with 'swap'.
 */
class FlipBuffer[T: ClassTag](val length: Int) {
  @volatile private var _front: Array[T] = Array.ofDim[T](length)
  @volatile private var _back: Array[T] = Array.ofDim[T](length)
  def front = _front
  def back = _back
  def update(idx: Int, value: T): Unit = { _back(idx) = value }
  def apply(idx: Int): T = { _front(idx) }
  def swap(): Unit = synchronized {
    val tmp = front
    _front = back
    _back = tmp
  }
}
