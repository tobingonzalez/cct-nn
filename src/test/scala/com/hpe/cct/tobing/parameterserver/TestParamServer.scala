package com.hpe.cct.tobing.parameterserver

import scala.swing.{Dimension, Swing}

import com.hpe.cct.tobing.debugger.MultiGraphDebugger

/** Test harness to launch the multigraph debugger UI on distributed model.
  *
  * Created by tobing on 6/3/16.
  */
object TestParamServer {
  def main(args: Array[String]): Unit = {
    val nGraphs = 2

    val lr = TestParamServerGraph.defaultLearningRule
    val bs = TestParamServerGraph.defaultBatchSize
    val serverRule = new ParameterServerLearningRule(lr)

    val graphs = Array.tabulate(nGraphs) { i =>
      libcog.Random.setDeterministic()
      new TestParamServerGraph(serverRule, bs, i, nGraphs)
    }

    Swing.onEDT {
      val debugger = new MultiGraphDebugger(graphs, serverRule.servers.toSeq)
      debugger.ui.preferredSize = new Dimension(800, 600)
      debugger.pack()
      debugger.visible = true
    }
  }
}

object Test {
  def main(args: Array[String]): Unit = {
    val lr = TestParamServerGraph.defaultLearningRule
    val bs = TestParamServerGraph.defaultBatchSize
    val serverRule = new ParameterServerLearningRule(lr)
    val graph = new TestParamServerGraph(serverRule, bs, 0, 1)
    graph.reset
    println("graph reset successfully")
    graph.step
    println("graph stepped")
    graph.release
    println("graph released")
  }
}
