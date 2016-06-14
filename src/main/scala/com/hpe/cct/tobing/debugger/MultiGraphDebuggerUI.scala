package com.hpe.cct.tobing.debugger

import java.util.concurrent.{TimeUnit, ScheduledThreadPoolExecutor, Executors}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.swing.TabbedPane.Page
import scala.swing._

import com.hpe.cct.tobing.parameterserver.ParameterServer

import libcog.ComputeGraph
import cogdebugger.ui.components.ToolBar

/**
  * Created by tobing on 6/3/16.
  */
class MultiGraphDebuggerUI(
    graphs: Seq[ComputeGraph],
    servers: Option[Seq[ParameterServer]])
  extends BorderPanel {

  def this(graphs: Seq[ComputeGraph]) = this(graphs, None)

  val fieldButtons = new ButtonPanel(graphs)
  val tabPane = new TabbedPane()
  val desktops = graphs.map(new ViewerDesktop(_))
  desktops.zipWithIndex.foreach { case (desktop, idx) =>
    desktop.listenTo(fieldButtons.buttons)
    tabPane.pages += new Page(s"Graph $idx", desktop)
  }

  val split = new SplitPane(Orientation.Vertical, fieldButtons, tabPane)

  @volatile private var stepCount = 0L
  val counter = new TextField(7)
  counter.maximumSize = new Dimension(100, counter.maximumSize.height)
  counter.editable = false
  val counterUpdater = new ScheduledThreadPoolExecutor(1)
  counterUpdater.scheduleAtFixedRate(
    new Runnable {
      override def run(): Unit = {
        counter.text = stepCount.toString
      }
    }, 0L, 1000 / 20, TimeUnit.MILLISECONDS
  )

  val toolbar = {
    val stepButton = Button("Step")(singleStep())
    val resetButton = Button("Reset")(resetGraphs())
    val probeButton = Button("Probe")(desktops.foreach(_.readProbes()))
    val runButton = new ToggleButton()
    runButton.action = Action("Run") {
      runButton.selected match {
        case true  => runGraphs()
        case false => stopRunning()
      }
    }
    val bar = new ToolBar("Tools", Orientation.Horizontal)
    bar.floatable = false
    bar.contents += stepButton
    bar.contents += resetButton
    bar.addSeparator()
    bar.contents += runButton
    bar.addSeparator()
    bar.contents += probeButton
    bar.contents += Swing.HGlue
    bar.contents += new Label("Step #: ")
    bar.contents += counter
    bar
  }

  add(toolbar, BorderPanel.Position.North)
  add(split, BorderPanel.Position.Center)

  implicit private val ec = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(2)
    override def execute(runnable: Runnable) { threadPool.submit(runnable) }
    override def reportFailure(t: Throwable) {}
  }

  @volatile private var stepInProgress = false
  private def singleStep(): Unit = {
    if (!stepInProgress) {
      stepInProgress = true
      val f = Future.traverse(graphs) { graph => Future { graph.step } }
      f.onComplete { case _ =>
        advanceServers()
        stepCount += 1
        stepInProgress = false
      }
    }
  }

  private def resetGraphs(): Unit = {
    val f = Future.traverse(graphs) { graph => Future(graph.reset) }
    Await.result(f, 5 seconds)
    advanceServers()
    stepCount = 0L
  }

  private def advanceServers(): Unit =
    for (serverList <- servers; server <- serverList) {
      server.prepareForNextStep()
    }

  private var runner: Runner = null
  private def runGraphs(): Unit = {
    if (runner == null) {
      runner = new Runner()
      val t = new Thread(runner)
      t.start()
    }
  }

  private def stopRunning(): Unit = {
    if (runner != null) {
      runner.running = false
      runner = null
    }
  }

  private class Runner extends Runnable {
    @volatile var running = false
    override def run(): Unit = {
      running = true
      while (running && !Thread.interrupted()) {
        val f = Future.traverse(graphs) { graph => Future { graph.step } }
        Await.result(f, 5 seconds)
        advanceServers()
        stepCount += 1
      }
    }
  }

}
