package com.hpe.cct.tobing.parameterserver

import libcog._
import toolkit.neuralnetwork.DifferentiableField
import toolkit.neuralnetwork.examples.util.{DataAugmentation, VectorMeanSquares}
import toolkit.neuralnetwork.function._
import toolkit.neuralnetwork.layer.{FullyConnectedLayer, ConvolutionLayer}
import toolkit.neuralnetwork.policy.{Space, StandardLearningRule}
import toolkit.neuralnetwork.source.{FloatLabelSource, ByteDataSource, RandomSource}
import toolkit.neuralnetwork.util.{NormalizedLowPass, CorrectCount}
import toolkit.neuralnetwork.Implicits._

/**
  * Created by tobing on 6/22/16.
  *
  * @param lr Learning rule compatible with a parameter server
  * @param bs Aggregate mini-batch size across the whole cluster
  * @param graphIdx This client's unique index, from [0-numGraphs)
  * @param numGraphs Number of clients participating in the computation
  * @param tuneForMaxwell Enable optimizations specific to NVidia Maxwell
  * @param useRandomData Use random data or real data?
  */
class DistributedAlexNet(
    lr: ParameterServerLearningRule,
    bs: Int,
    graphIdx: Int,
    numGraphs: Int,
    tuneForMaxwell: Boolean = false,
    val useRandomData: Boolean = true)
  extends ComputeGraph {

  // Network parameters ///////////////////////////////////////////////////////
  val batchSize = bs / numGraphs
  // Include AlexNet normalization layers?
  val enableNormalization = true
  // Parameters for AlexNet normalization layers
  val (k, alpha, beta, windowSize) = (2.0f, 1e-4f, 0.75f, 5)

  // Data parameters //////////////////////////////////////////////////////////
  // Paths to the mean image file, training images, and training labels for
  // real data option
  val imagenetRoot = "/fdata/scratch/imagenet/"
  val meanImageFile = imagenetRoot + "TrainingMeanImage1.bin"
  val trainingImages = imagenetRoot + "TrainingImages1.bin"
  val labelFile = imagenetRoot + "TrainingLabels1.bin"

  // Tuning parameters ////////////////////////////////////////////////////////
  // Use Maxwell-optimized convolution? Set to false on Kepler or prior
  // architectures.
  Convolution.tuneForNvidiaMaxwell = tuneForMaxwell

  /////////////////////////////////////////////////////////////////////////////

  def normalize(in: DifferentiableField): DifferentiableField = {
    if (!enableNormalization) {
      in
    } else {
      in * AplusBXtoN(VectorMeanSquares(in, windowSize, BorderCyclic), a = k, b = 5 * alpha, n = -beta)
    }
  }

  val data: DifferentiableField = if (useRandomData) {
    RandomSource(Shape(230, 230), 3, batchSize)
  } else {
    val meanImage = DataAugmentation.loadOffsetVector(meanImageFile)

    def meanImageAsVectorField: VectorField = {
      VectorField(meanImage.length, meanImage(0).length, (i, j) => meanImage(i)(j))
    }

    // Load 256x256x3 samples from disk
    val raw = ByteDataSource(trainingImages, Shape(256, 256), 3, batchSize, offset = graphIdx, stride = numGraphs)
    // Subtract the mean image and apply a random crop and reflection
    val pre1 = DataAugmentation.subtractCropReflect2(raw.forward, meanImageAsVectorField, Shape(230, 230))
    // Apply the AlexNet color shift data augmentation
    val pre2 = DifferentiableField(DataAugmentation.applyColorShiftPerImage(pre1, batchSize), batchSize)

    pre2
  }

  val label: DifferentiableField = if (useRandomData) {
    RandomSource(Shape(), 1000, batchSize)
  } else {
    FloatLabelSource(labelFile, 1000, batchSize, offset = graphIdx, stride = numGraphs)
  }

  val c1 = ConvolutionLayer(data, Shape(11, 11), 96, BorderValid, lr, stride = 4, impl = Space)
  val r1 = ReLU(c1)
  val n1 = normalize(r1)
  val p1 = MaxPooling(n1, poolSize = 3, stride = 2)

  val c2 = ConvolutionLayer(p1, Shape(5, 5), 256, BorderZero, lr)
  val r2 = ReLU(c2)
  val n2 = normalize(r2)
  val p2 = MaxPooling(n2, poolSize = 3, stride = 2)

  val c3 = ConvolutionLayer(p2, Shape(3, 3), 384, BorderZero, lr)
  val r3 = ReLU(c3)

  val c4 = ConvolutionLayer(r3, Shape(3, 3), 384, BorderZero, lr)
  val r4 = ReLU(c4)

  val c5 = ConvolutionLayer(r4, Shape(3, 3), 256, BorderZero, lr)
  val r5 = ReLU(c5)
  val p5 = MaxPooling(r5, poolSize = 3, stride = 2)

  val fc6 = FullyConnectedLayer(p5, 4096, lr)
  val d6 = Dropout(fc6)
  val r6 = ReLU(d6)

  val fc7 = FullyConnectedLayer(r6, 4096, lr)
  val d7 = Dropout(fc7)
  val r7 = ReLU(d7)

  val fc8 = FullyConnectedLayer(r7, 1000, lr)

  val loss = CrossEntropySoftmax(fc8, label) / batchSize

  loss.activateSGD()

  val correct = CorrectCount(fc8.forward, label.forward, batchSize, 0.01f) / batchSize
  val avgCorrect = NormalizedLowPass(correct, 0.001f)
  val avgLoss = NormalizedLowPass(loss.forward, 0.001f)

  def enableProbes(): Unit = {
    probe(data.forward)
    probe(label.forward)
    probe(loss.forward)
    probe(correct)
    probe(avgCorrect)
    probe(avgLoss)
  }
}

object DistributedAlexNet {
  /** Mini-batch size */
  val DefaultBatchSize = 128
  /** Weight update rule and parameters */
  val DefaultLearningRule = StandardLearningRule(0.01f, 0.9f, 0.0005f)
}
