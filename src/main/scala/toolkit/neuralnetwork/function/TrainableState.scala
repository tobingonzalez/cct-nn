/*
 * (c) Copyright 2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package toolkit.neuralnetwork.function

import libcog._
import toolkit.neuralnetwork.DifferentiableField
import toolkit.neuralnetwork.policy.{LearningRule, WeightInitPolicy}


case class TrainableState(fieldShape: Shape, tensorShape: Shape, initPolicy: WeightInitPolicy, learningRule: LearningRule) extends DifferentiableField {
  val initState = initPolicy.initState(fieldShape, tensorShape)
  override val batchSize = 1
  override val gradientConsumer = learningRule.gradientConsumer
  override val forward: Field = initState.toField

  override def backwardCallback(backward: Field): Unit = learningRule.learn(forward, backward)
}
