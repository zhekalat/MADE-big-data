package org.apache.spark.ml.made

import breeze.linalg.DenseVector
import com.google.common.io.Files
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.made.models.{LinearRegression, LinearRegressionModel}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, lit, rand}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should


class LinearRegressionTest extends AnyFlatSpec with should.Matchers with WithSpark {

    lazy val data: DataFrame = LinearRegressionTest._data
    lazy val vectors: Seq[Vector] = LinearRegressionTest._vectors
    lazy val train_data: DataFrame = LinearRegressionTest._train_data
    lazy val train_points: Seq[Vector] = LinearRegressionTest._train_points
    val delta: Double = 1e-4
    val weight_delta: Double = 1e-2
    val stepSize: Double = 1.0
    val numIterations: Int = 1000
    val weights: Vector = Vectors.dense(0.8, -0.3)
    val bias: Double = 1.6

    "Model" should "calculate a linear combination from the data" in {
        val model: LinearRegressionModel = new LinearRegressionModel(
            weights = weights.toDense,
            bias = bias
        ).setInputCol("features")
            .setOutputCol("prediction")

        val values = model.transform(data).collect().map(_.getAs[Double](1))

        values.length should be(data.count())

        values(0) should be(vectors(0)(0) * weights(0) + vectors(0)(1) * weights(1) + bias +- delta)
        values(1) should be(vectors(1)(0) * weights(0) + vectors(1)(1) * weights(1) + bias +- delta)
    }

    "Estimator" should "produce functional model" in {
        val true_model: LinearRegressionModel = new LinearRegressionModel(
            weights = weights.toDense,
            bias = bias
        ).setInputCol("features")
            .setOutputCol("label")

        val train = true_model
            .transform(train_data)
            .select(col("features"), (col("label") +
                rand() * lit(0.1) - lit(0.05)).as("label"))

        val estimator: LinearRegression = new LinearRegression(stepSize, numIterations)
            .setInputCol("features")
            .setOutputCol("label")
        val model = estimator.fit(train)

        model.weights(0) should be(weights(0) +- weight_delta)
        model.weights(1) should be(weights(1) +- weight_delta)
        model.bias should be(bias +- weight_delta)
    }

    "Model" should "work after reload" in {
        val true_model: LinearRegressionModel = new LinearRegressionModel(
            weights = weights.toDense,
            bias = bias
        ).setInputCol("features")
            .setOutputCol("label")

        val train = true_model
            .transform(train_data.select(col("features")))
            .select(
                col("features"),
                (col("label") + rand() * lit(0.1) - lit(0.05)).as("label")
            )

        var pipeline = new Pipeline().setStages(Array(
            new LinearRegression(stepSize, numIterations)
                .setInputCol("features")
                .setOutputCol("label")
        ))

        val tmpFolder = Files.createTempDir()
        pipeline.fit(train).write.overwrite().save(tmpFolder.getAbsolutePath)

        val pipeline_model = PipelineModel.load(tmpFolder.getAbsolutePath)
        val model = pipeline_model.stages(0).asInstanceOf[LinearRegressionModel]

        model.getInputCol should be("features")
        model.getOutputCol should be("label")
        model.weights(0) should be(weights(0) +- weight_delta)
        model.weights(1) should be(weights(1) +- weight_delta)
        model.bias should be(bias +- weight_delta)
    }

    "Estimator" should "work after reload" in {
        val true_model: LinearRegressionModel = new LinearRegressionModel(
            weights = weights.toDense,
            bias = bias
        ).setInputCol("features")
            .setOutputCol("label")

        val train = true_model
            .transform(train_data.select(col("features")))
            .select(
                col("features"),
                (col("label") + rand() * lit(0.1) - lit(0.05)).as("label")
            )

        var pipeline = new Pipeline().setStages(Array(
            new LinearRegression(stepSize, numIterations)
                .setInputCol("features")
                .setOutputCol("label")
        ))

        val tmpFolder = Files.createTempDir()
        pipeline.write.overwrite().save(tmpFolder.getAbsolutePath)

        pipeline = Pipeline.load(tmpFolder.getAbsolutePath)
        pipeline.getStages(0).asInstanceOf[LinearRegression].stepSize should be(stepSize)
        pipeline.getStages(0).asInstanceOf[LinearRegression].numIterations should be(numIterations)

        val model = pipeline.fit(train).stages(0).asInstanceOf[LinearRegressionModel]

        model.weights(0) should be(weights(0) +- weight_delta)
        model.weights(1) should be(weights(1) +- weight_delta)
        model.bias should be(bias +- weight_delta)
    }
}

object LinearRegressionTest extends WithSpark {
    import sqlc.implicits._

    lazy val _vectors = Seq(
        Vectors.dense(5.2, -1.2),
        Vectors.dense(1.8, 2.1),
    )
    lazy val _data: DataFrame = {
        _vectors.map(x => Tuple1(x)).toDF("features")
    }

    lazy val _train_points: Seq[Vector] = Seq.fill(1000)(Vectors.fromBreeze(DenseVector.rand(2)))
    lazy val _train_data: DataFrame = {
        _train_points.map(x => Tuple1(x)).toDF("features")
    }
}