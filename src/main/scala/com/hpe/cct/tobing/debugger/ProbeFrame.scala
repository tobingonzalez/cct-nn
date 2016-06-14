package com.hpe.cct.tobing.debugger

import org.interactivemesh.scala.swing.InternalFrame

import cogx.platform.cpumemory.FieldMemory
import cogx.runtime.debugger.ProbedField
import cogdebugger.ui.fieldvisualizations.Viewer

/**
  * Created by tobing on 6/7/16.
  */
case class ProbeFrame(field: ProbedField, viewer: Viewer) extends InternalFrame {
  @volatile var busy = false
  val readTarget = FieldMemory.direct(field.fieldType)
  closable = true
  resizable = true
  maximizable = true
  title = field.name.mkString(".")
  contents = viewer
  //viewer.listenTo(this)
}
