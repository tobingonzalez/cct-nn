package com.hpe.cct.tobing.debugger

import scala.swing._

import libcog.ComputeGraph

/**
  * Created by tobing on 6/7/16.
  */
class ButtonPanel(graphs: Seq[ComputeGraph])
  extends BoxPanel(Orientation.Vertical) {

  require(graphs.nonEmpty, "ButtonPanel created with no graphs")

  val launcherButtons = {
    val names = graphs.head.probedCircuit.flatten.map(_.name.mkString("."))
    val fields = graphs.map(_.probedCircuit.flatten).transpose
    val zipped = names zip fields
    zipped.map { case (name, fields) =>
        Button(name)(fields.foreach(f => buttons.publish(ProbeRequest(f))))
    }
  }

  // center buttons horizontally
  launcherButtons.foreach(_.xLayoutAlignment = 0.5)

  contents ++= launcherButtons

  object buttons extends Publisher
}
