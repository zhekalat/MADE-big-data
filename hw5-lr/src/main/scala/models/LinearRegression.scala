package models

import breeze.linalg.{*, DenseMatrix, DenseVector, sum}
import metrics.MSE
import optimizers.SGD

case class LinearRegression() {
    private var weights: DenseVector[Double] = DenseVector.rand(1)
    private var bias: Double = DenseVector.rand(1).data(0)

    def predict(X: DenseMatrix[Double]): DenseVector[Double] = {
        val X_weighted = X(*, ::) * this.weights
        sum(X_weighted(*, ::)) + this.bias
    }

    def fit(features: DenseMatrix[Double], target: DenseVector[Double], optimizer: SGD, epochs: Int): Unit = {
        this.weights = DenseVector.rand(features.cols)

        // Split data to train and val subsets
        val X = features(0 to (0.8 * features.rows).toInt, ::)
        val X_val = features((0.8 * features.rows).toInt to -1, ::)
        val y = target(0 to (0.8 * target.length).toInt)
        val y_val = target((0.8 * target.length).toInt to -1)

        for (i <- 0 until epochs) {
            // Compute predictions and loss gradient
            val y_pred = this.predict(X)
            val (grad_weights, grad_bias) = optimizer.gradient(X, y, y_pred)

            // Update weights
            this.weights = this.weights - grad_weights
            this.bias = this.bias - grad_bias

            // Validation score
            val y_pred_val = this.predict(X_val)
            if (i % 50 == 0) {
                print(i.toString + ": " + MSE(y_val, y_pred_val).toString + "\n")
            }
        }
    }
}
