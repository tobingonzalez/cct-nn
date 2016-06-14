package com.hpe.cct.tobing.debugger

import scala.swing.event.Event

import cogx.runtime.debugger.ProbedField

/**
  * Created by tobing on 6/7/16.
  */
case class ProbeRequest(probedField: ProbedField) extends Event
