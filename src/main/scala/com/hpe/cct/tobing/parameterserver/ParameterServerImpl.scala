package com.hpe.cct.tobing.parameterserver

import com.hpe.cct.tobing.util.FlipBuffer

import libcog.{Field, FieldType, ScalarFieldReader}

/** A very simple parameter server implementation. Only allows one client to
  * update the back buffer at a time. It is intended to be used to accumulate
  * and distribute the backpropagation signal from a number of clients.
  *
  * This server does not implement a learning rule, it only accumulates and
  * distributes weights. Application of those weights to a client's model
  * (that is, the learning) is left to the client.
  *
  * In general, this is unsafe. If the clients are homogeneous (same hardware,
  * same software), their models *should* evolve identically, as their learning
  * rules will be working from the same backprop signals. If they are not
  * homogeneous, you might expect their models to drift apart over time (e.g.
  * due to floating point rounding error).
  *
  * Created by tobing on 6/3/16.
  */
class ParameterServerImpl(fieldType: FieldType) extends ParameterServer {

  def this(field: Field) = this({
    println("building server for field: "+field)
    field.fieldType
  })

  private val length = fieldType.fieldShape.points * fieldType.tensorShape.points
  println("created server for field with fieldType: "+fieldType)

  private val weights = new FlipBuffer[Float](length)

  override def push(field: ScalarFieldReader): Unit = synchronized {
    val it = field.iterator
    var i = 0
    val len = weights.length
    while (i < len) {
      weights(i) += it.next()
      i += 1
    }
    require(!it.hasNext) // sanity check
  }

  override def pull(): Array[Float] = weights.front

  override def prepareForNextStep(): Unit = {
    weights.swap()
    java.util.Arrays.fill(weights.back, 0f)
  }
}
