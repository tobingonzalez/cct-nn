package com.hpe.cct.tobing.parameterserver

import scala.collection.mutable

import libcog.{Field, Shape, Sensor}
import cogx.compiler.parser.syntaxtree.Actuator
import toolkit.neuralnetwork.policy.LearningRule

/** Wraps a learning rule for use with ParameterServerImpl.
  *
  * Normally, the learning rule is fed the forward and backward signals from
  * a single compute graph/model. This wrapper intercepts the backward signal
  * and forwards it to a parameter server, where it is aggregated with the
  * signals from other models. The aggregated signal is then distributed by
  * the server to its clients, where it can be fed into wrapped learning rule.
  *
  * Note that in this scheme the clients are still responsible for applying the
  * backprop signal to their learned state. If the clients are not homogeneous,
  * this can lead to drift in their learned state over time (due to floating
  * point rounding error, etc.). An alternative is to have the parameter server
  * implement the learning rule and distribute the learned state to clients,
  * not just the backprop signal.
  *
  * Created by tobing on 6/3/16.
  */
class ParameterServerLearningRule(
    wrappedRule: LearningRule)
  extends LearningRule {

  private val serverMap = mutable.Map.empty[String, ParameterServer]

  def servers = serverMap.values

  override val gradientConsumer = true

  override def learn(forward: Field, backward: Field): Unit = {
    val ft = backward.fieldType
    val fs = ft.fieldShape
    val ts = ft.tensorShape
    val points = fs.points * ts.points

    println("Getting server for field "+backward+" (long name: "+backward.name+")")
    val server = getServerFor(backward)

    val flattened = backward.reshape(Shape(points), Shape())
    val act = Actuator(flattened, server.push(_))

    val sensor = new Sensor(points, () => Some(server.pull()))
    val inflated = sensor.reshape(fs, ts)

    wrappedRule.learn(forward, inflated)
  }

  private def getServerFor(field: Field): ParameterServer = {
    field.name
    val fieldName = field.name match {
      case "" => field.toString()
      case s => s
    }
    serverMap.getOrElseUpdate(fieldName, new ParameterServerImpl(field))
  }
}
