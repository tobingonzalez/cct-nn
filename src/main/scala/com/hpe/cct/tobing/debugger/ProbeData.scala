package com.hpe.cct.tobing.debugger

import scala.swing.event.Event

import libcog.AbstractFieldMemory
import cogx.runtime.debugger.ProbedField

/**
  * Created by tobing on 6/10/16.
  */
case class ProbeData(src: ProbedField, data: AbstractFieldMemory) extends Event
