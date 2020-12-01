import breeze.linalg.{*, DenseMatrix, DenseVector, sum}
import breeze.stats.{mean, stddev}

val x: Int = 5
print(x)

DenseVector.rand(3)

val X: DenseMatrix[Double] = DenseMatrix.rand[Double](5,2)
val bias: Double = 5
val weights: DenseVector[Double] = DenseVector[Double](2, 3)

val X_w = X(*, ::) * weights

sum(X_w(*, ::)) + bias

X(0 to (0.8 * X.rows).toInt, ::)