package normalizers

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.stats.{mean, stddev}

case class StandardScaler() {
    private var feature_mean: Double = Double.NaN
    private var feature_std: Double = Double.NaN

    def fit(samples: DenseVector[Double]): Unit = {
        this.feature_mean = mean(samples)
        this.feature_std = stddev(samples)
    }

    def transform(samples: DenseVector[Double]): DenseVector[Double] = {
        (samples - this.feature_mean) / this.feature_std
    }

    def reverse_transform(samples: DenseVector[Double]): DenseVector[Double] = {
        samples * this.feature_std + this.feature_mean
    }

    def fit_transform(samples: DenseVector[Double]): DenseVector[Double] = {
        this.feature_mean = mean(samples)
        this.feature_std = stddev(samples)
        (samples - this.feature_mean) / this.feature_std
    }
}
