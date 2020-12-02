package org.apache.spark.ml.made.optimizers

import breeze.linalg.{axpy, sum, Vector => BreezeVector}
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.param.shared.{HasInputCol, HasOutputCol}
import org.apache.spark.ml.param.{ParamMap, Params}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.expressions.Aggregator
import org.apache.spark.sql.{Dataset, Encoder, Row}


trait GradientDescentParams extends HasInputCol with HasOutputCol {
    def setInputCol(value: String): this.type = set(inputCol, value)
    def setOutputCol(value: String): this.type = set(outputCol, value)

    setDefault(inputCol, "features")
    setDefault(outputCol, "label")
}


case class GradientDescent private[made](override val uid: String,
                                         private var stepSize: Double,
                                         private var numIterations: Int) extends GradientDescentParams {

    def this() = this(Identifiable.randomUID("GradientDescend"), 1e-3, 1000)
    def this(uid: String) = this(uid, 1e-3, 1000)
    def this(stepSize: Double, numIterations: Int) = this(
        Identifiable.randomUID("GradientDescend"),
        stepSize,
        numIterations)

    def optimize(dataset: Dataset[_], initialWeights: Vector): Vector = {
        var weights = Vectors.dense(initialWeights.toArray)
        val weight_count = weights.size
        val row_count = dataset.count()

        def gradient(data: Vector, label: Double, weights: Vector): Vector = {
            val loss = sum(data.asBreeze *:* weights.asBreeze) - label
            val gradient = data.copy.asBreeze * loss
            Vectors.fromBreeze(gradient)
        }

        val customSummer: Aggregator[Row, Vector, Vector] = new Aggregator[Row, Vector, Vector] {
            def zero: Vector = Vectors.zeros(weight_count)

            def reduce(acc: Vector, x: Row): Vector = {
                val grad = gradient(x.getAs[Vector]($(inputCol)), x.getAs[Double]($(outputCol)), weights)
                Vectors.fromBreeze(acc.asBreeze + grad.asBreeze / row_count.asInstanceOf[Double])
            }

            def merge(acc1: Vector, acc2: Vector): Vector = Vectors.fromBreeze(acc1.asBreeze + acc2.asBreeze)

            def finish(r: Vector): Vector = r

            override def bufferEncoder: Encoder[Vector] = ExpressionEncoder()

            override def outputEncoder: Encoder[Vector] = ExpressionEncoder()
        }

        for (i <- 1 until numIterations) {
            val gradient = dataset.select(customSummer.toColumn.as[Vector](ExpressionEncoder())).first().asBreeze

            val DenseWeights: BreezeVector[Double] = weights.asBreeze.toDenseVector
            axpy(-stepSize, gradient, DenseWeights)
            weights = Vectors.fromBreeze(DenseWeights)

            if (i % 100 == 0) {
                this.stepSize = this.stepSize / 2
            }
        }
        weights
    }

    override def copy(extra: ParamMap): Params = {
        copyValues(new GradientDescent(stepSize, numIterations))
    }
}
