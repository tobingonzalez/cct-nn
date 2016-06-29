package com.hpe.cct.tobing.parameterserver

import java.io.File

import libcog.{ComputeGraph, Shape}
import toolkit.neuralnetwork.Implicits._
import toolkit.neuralnetwork.function.CrossEntropySoftmax
import toolkit.neuralnetwork.layer.{BiasLayer, FullyConnectedLayer}
import toolkit.neuralnetwork.policy.{LearningRule, StandardLearningRule}
import toolkit.neuralnetwork.source.{ByteDataSource, ByteLabelSource}
import toolkit.neuralnetwork.util.{CorrectCount, NormalizedLowPass}

/** The logistic regression example modified to work as a distributed model.
  * Used as a test case in the development of the multigraph debugger and
  * parameter server classes.
  *
  * Created by tobing on 6/3/16.
  */
class DistributedLogisticRegression(
    lr: LearningRule,
    bs: Int,
    graphIdx: Int,
    numGraphs: Int)
  extends ComputeGraph {

  val batchSize = bs / numGraphs

  val dir = new File(System.getProperty("user.home"), "cog/data/MNIST")

  val data = ByteDataSource(new File(dir, "train-images.idx3-ubyte").toString,
    Shape(28, 28), 1, batchSize, headerLen = 16, offset = graphIdx, stride = numGraphs)

  val label = ByteLabelSource(new File(dir, "train-labels.idx1-ubyte").toString,
    10, batchSize, headerLen = 8, offset = graphIdx, stride = numGraphs)

  val b = BiasLayer(data, lr)
  val fc = FullyConnectedLayer(b, 10, lr)
  val loss = CrossEntropySoftmax(fc, label)
  //val normLoss = loss / batchSize // Original definition
  // For whatever reason, unless we use the aggregate batch size across the
  // parameter server group, the model will eventually produce NaNs and die.
  // The model is stable using the aggregate batch size (learning parameters
  // are tuned for the aggregate size, maybe?)
  val normLoss = loss / bs//batchSize
  val correct = CorrectCount(fc.forward, label.forward, batchSize, 0.01f) / batchSize
  val avgCorrect = NormalizedLowPass(correct, 0.001f)
  val avgLoss = NormalizedLowPass(normLoss.forward, 0.001f)

  normLoss.activateSGD()
}

object DistributedLogisticRegression {
  val DefaultLearningRule = StandardLearningRule(0.01f, 0.9f, 0.0005f)
  val DefaultBatchSize = 120
}
