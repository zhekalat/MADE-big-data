package metrics

import breeze.linalg.{DenseVector, sum}
import breeze.numerics.pow
import breeze.stats.mean

object R2 {
    def apply(y_true: DenseVector[Double], y_pred: DenseVector[Double]): Double = {
        val ssTot = sum(pow(y_true - mean(y_true), 2))
        val ssRes = sum(pow(y_true - y_pred, 2))
        1 - ssRes / ssTot
    }
}
