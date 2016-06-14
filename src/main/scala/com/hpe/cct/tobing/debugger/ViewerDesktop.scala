package com.hpe.cct.tobing.debugger

import java.util.concurrent.{ScheduledFuture, TimeUnit, ScheduledThreadPoolExecutor}

import scala.swing.Dimension
import scala.swing.event.{Event, UIElementResized}

import org.interactivemesh.scala.swing.LayeredPane.LayerConstraints
import org.interactivemesh.scala.swing.event.{InternalFrameClosed, InternalFrameOpened}
import org.interactivemesh.scala.swing.{InternalDesktopPane, InternalFrame}

import libcog.{ComputeGraph, Float32, Complex32, Uint8Pixel}
import cogx.runtime.debugger.ProbedField
import cogdebugger.BinPacker2D
import cogdebugger.coggui3ports.FramePackedEvent
import cogdebugger.ui.fieldvisualizations.{Viewer, _}

/**
  * Created by tobing on 6/7/16.
  */
class ViewerDesktop(
    graph: ComputeGraph,
    fps: Int)
  extends InternalDesktopPane {

  ////////////////////////////////////////////////////////////////////////////
  // Alternate constructors

  def this(graph: ComputeGraph) = this(graph, 20)

  ////////////////////////////////////////////////////////////////////////////
  // Initialization

  private val Self = this // identifier used in a pattern match below
  private val tiler = new BinPacker2D(size)

  private val probeTask = new Runnable { override def run(): Unit = readProbes() }
  private val probeDriver = new ScheduledThreadPoolExecutor(1)
  private var future: ScheduledFuture[_] = null

  dragMode = InternalDesktopPane.DragMode.Outline
  reschedule(fps)
  reactions += {
    case ProbeRequest(field) =>
      if (graph.probedCircuit.contains(field)) launchProbe(field)
    case UIElementResized(`Self`) =>
      tiler.reset(size)
      for (frame <- contents) { frame.maximumSize = this.size }
    case FramePackedEvent(src) =>
      val rect = tiler.insert(src.size, src)
      rect.foreach(r => src.location = (r.x, r.y))
    case InternalFrameOpened(src, param) =>
      val rect = tiler.insert(src.size, src)
      rect.foreach(r => src.location = (r.x, r.y))
    case InternalFrameClosed(src, param) =>
      tiler.remove(src)
      deafTo(src)
  }
  listenTo(this)

  ////////////////////////////////////////////////////////////////////////////
  // Private methods

  private[debugger] def add(frame: InternalFrame): Unit =
    this.add(frame, new LayerConstraints())

  private def launchProbe(field: ProbedField): Unit = {
    try {
      val viewer = makeViewer(field)
      val frame = new ProbeFrame(field, viewer)

      listenTo(frame)
      frame.maximumSize = this.size
      add(frame)
      frame.pack
      limitSize(frame)
      frame.visible = true
    } catch {
      case e: MatchError => Console.err.println("Unable to launch viewer for "+field)
    }
  }

  private def limitSize(frame: InternalFrame): Unit = {
    val sz = frame.size
    val newW = if (sz.width > size.width) size.width else sz.width
    val newH = if (sz.height > size.height) size.height else sz.height
    frame.peer.setSize(new Dimension(newW, newH))
  }

  private def makeViewer(field: ProbedField): Viewer = {
    field.fieldType.elementType match {
      case Float32 => makeFloatViewer(field)
      case Complex32 => makeComplexViewer(field)
      case Uint8Pixel => makeByteViewer(field)
    }
  }

  private def makeFloatViewer(field: ProbedField): Viewer = {
    field.fieldType.tensorOrder match {
      case 0 => ScalarFieldSuperPanel(field)
      case 1 => VectorFieldSuperPanel(field)
      case 2 => MatrixFieldSuperPanel(field)
    }
  }

  private def makeComplexViewer(field: ProbedField): Viewer = {
    ???
  }

  private def makeByteViewer(field: ProbedField): Viewer = {
    field.fieldType.tensorOrder match {
      case 1 => ColorFieldSuperPanel(field)
    }
  }

  private[debugger] def reschedule(fps: Int): Unit = {
    require(fps > 0)
    if (future != null) future.cancel(true)
    future = probeDriver.scheduleAtFixedRate(probeTask, 0, 1000 / fps, TimeUnit.MILLISECONDS)
  }

  private[debugger] def readProbes(): Unit = {
    for (child <- contents) child match {
      case pf @ ProbeFrame(field, viewer) =>
        if (!pf.busy) {
          pf.busy = true
          graph.read(field, pf.readTarget, (field, mem) => {
            viewer.update(field, mem, 0L)
            pf.busy = false
          })
        }
      case _ => // Not a probe?
    }
  }
}


