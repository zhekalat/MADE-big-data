package optimizers

import breeze.linalg.{DenseMatrix, DenseVector, sum}
import scala.util.Random

case class SGD(learning_rate: Double, batch_size: Int) {
      def gradient(X: DenseMatrix[Double], y_true: DenseVector[Double], y_pred: DenseVector[Double]):
      (DenseVector[Double], Double) = {
          // Select random batch rows
          val rand = Random
          val indexes = for (i <- 0 to this.batch_size) yield rand.nextInt(X.rows)
          val batchX = X(indexes, ::)
          val batch_y_true = y_true(indexes).toDenseVector
          val batch_y_pred = y_pred(indexes).toDenseVector

          // Compute loss and gradients by each weight
          val loss: DenseVector[Double] = batch_y_true - batch_y_pred
          var grad_weights: DenseVector[Double] = DenseVector.zeros[Double](batchX.cols)
          for (i <- 0 until batchX.cols) {
              grad_weights(i) = -2 / this.batch_size * sum(loss *:* batchX(::, i))
          }
          var grad_bias: Double = sum(-2 / this.batch_size.toDouble * loss)

          // Multiply gradients by learning rate
          grad_weights = grad_weights * this.learning_rate
          grad_bias = grad_bias * this.learning_rate

          (grad_weights, grad_bias)
      }
}
