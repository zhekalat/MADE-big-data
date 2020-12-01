package metrics

import breeze.linalg.DenseVector
import breeze.numerics.pow
import breeze.stats.mean

object MSE {
    def apply(y_true: DenseVector[Double], y_pred: DenseVector[Double]): Double = {
        mean(pow(y_true - y_pred, 2))
    }
}
