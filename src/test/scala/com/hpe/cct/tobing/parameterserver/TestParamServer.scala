package com.hpe.cct.tobing.parameterserver

import scala.swing.{Dimension, Swing}

import com.hpe.cct.tobing.debugger.MultiGraphDebugger

import libcog.ComputeGraph

/** Test harness to launch the multigraph debugger UI on distributed model.
  *
  * Created by tobing on 6/3/16.
  */
class TestHarness(graphs: Seq[ComputeGraph], lr: ParameterServerLearningRule) {
  def main(args: Array[String]): Unit = {
    Swing.onEDT {
      val debugger = new MultiGraphDebugger(graphs, lr.servers.toSeq)
      debugger.ui.preferredSize = new Dimension(800, 600)
      debugger.pack()
      debugger.visible = true
    }
  }
}

object TestDistributedLogisticRegression extends {
  val nGraphs = 2
  val lr = DistributedLogisticRegression.DefaultLearningRule
  val bs = DistributedLogisticRegression.DefaultBatchSize * nGraphs
  val serverRule = new ParameterServerLearningRule(lr)
  val graphs = Array.tabulate(nGraphs) { i =>
    libcog.Random.setDeterministic()
    val g = new DistributedLogisticRegression(serverRule, bs, i, nGraphs)
    libcog.probe(g.data.forward)
    g
  }
} with TestHarness(graphs, serverRule)

object TestDistributedAlexNet extends {
  val nGraphs = 2
  val lr = DistributedAlexNet.DefaultLearningRule
  val bs = DistributedAlexNet.DefaultBatchSize
  val serverRule = new ParameterServerLearningRule(lr)
  val graphs = Array.tabulate(nGraphs) { i =>
    libcog.Random.setDeterministic()
    new DistributedAlexNet(serverRule, bs, i, nGraphs, useRandomData = true)
  }
} with TestHarness(graphs, serverRule)
