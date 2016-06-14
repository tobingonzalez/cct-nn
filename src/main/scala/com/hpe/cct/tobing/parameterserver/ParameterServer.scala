package com.hpe.cct.tobing.parameterserver

import libcog.ScalarFieldReader

/** A Parameter Server is a device for sharing state between multiple compute
  * graphs. It's commonly used as part of a distributed learning technique in
  * which a single model is replicated many times, with each model working on
  * a different part of a single large training data set. The updates that an
  * individual model would make to its state (as part of back propagation) are
  * are forwarded to the parameter server, where they are aggregated and then
  * distributed to all models.
  *
  * Created by tobing on 6/3/16.
  */
trait ParameterServer {

  /** Push updates from an individual model to the parameter, to be
    * aggregated */
  def push(field: ScalarFieldReader): Unit

  /** Fetch the server's current state/parameters. */
  def pull(): Array[Float]

  /** Perform any server-side work needed to complete model state aggregation
    * and prepare for the next (global) model step. In a distributed
    * synchronous computation, this is called after all models have pushed
    * updates for tick i to the server, and before tick i+1 begins.
    */
  def prepareForNextStep(): Unit

}
