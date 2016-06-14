package com.hpe.cct.tobing.debugger

import scala.swing.MainFrame

import com.hpe.cct.tobing.parameterserver.ParameterServer

import libcog.ComputeGraph

/** An experimental debugger for CCT models involving one or more parameter
  * servers. Similar to the vanilla visual debugger, except that the probe
  * desktop area contains multiple tabs, one per graph, so that the state of
  * all graphs can be viewed with a single debugger instance.
  *
  * Created by tobing on 6/3/16.
  */
class MultiGraphDebugger(
    graphs: Seq[ComputeGraph],
    parameterServers: Option[Seq[ParameterServer]])
  extends MainFrame {

  def this(graphs: Seq[ComputeGraph]) = this(graphs, None)
  def this(graphs: Seq[ComputeGraph], servers: Seq[ParameterServer]) = this(graphs, Some(servers))

  val ui = new MultiGraphDebuggerUI(graphs, parameterServers)
  contents = ui
}
