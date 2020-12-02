package org.apache.spark.ml.made.models


import breeze.linalg.{sum, DenseVector => BreezeVector}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.{DenseVector, Vector, VectorUDT, Vectors}
import org.apache.spark.ml.made.optimizers.GradientDescent
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.param.shared.{HasInputCol, HasOutputCol}
import org.apache.spark.ml.util._
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.sql.functions.{col, lit}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset}


trait LinearRegressionParams extends HasInputCol with HasOutputCol {
    def setInputCol(value: String): this.type = set(inputCol, value)
    def setOutputCol(value: String): this.type = set(outputCol, value)

    setDefault(inputCol, "features")
    setDefault(outputCol, "label")

    protected def validateAndTransformSchema(schema: StructType): StructType = {
        SchemaUtils.checkColumnType(schema, getInputCol, new VectorUDT())

        if (schema.fieldNames.contains($(outputCol))) {
            SchemaUtils.checkNumericType(schema, getOutputCol)
            schema
        } else {
            SchemaUtils.appendColumn(schema, schema(getInputCol).copy(name = getOutputCol))
        }
    }
}


class LinearRegression(override val uid: String,
                       val stepSize: Double,
                       val numIterations: Int)
    extends Estimator[LinearRegressionModel]
        with LinearRegressionParams
        with DefaultParamsWritable
        with MLWritable {

    private val optimizer = new GradientDescent(stepSize, numIterations)
        .setInputCol($(inputCol))
        .setOutputCol($(outputCol))

    def this() = this(Identifiable.randomUID("LinearRegression"), 1e-3, 1000)
    def this(uid: String) = this(uid, 1e-3, 1000)
    def this(stepSize: Double, numIterations: Int) = this(
        Identifiable.randomUID("LinearRegression"),
        stepSize,
        numIterations)

    override def fit(dataset: Dataset[_]): LinearRegressionModel = {
        var weights: Vector = Vectors.fromBreeze(BreezeVector.rand(dataset.columns.length + 1))

        val featuresWithBias = dataset.withColumn("ones", lit(1))
        val featuresAssembled = new VectorAssembler()
            .setInputCols(Array($(inputCol), "ones"))
            .setOutputCol("features_extended")
            .transform(featuresWithBias)
            .select(col("features_extended").as($(inputCol)), col($(outputCol)))

        weights = optimizer.optimize(featuresAssembled, weights)

        copyValues(new LinearRegressionModel(new DenseVector(
            weights.toArray.slice(0, weights.size - 1)),
            weights.toArray(weights.size - 1)))
            .setParent(this)
    }

    override def copy(extra: ParamMap): Estimator[LinearRegressionModel] = {
        copyValues(new LinearRegression(stepSize, numIterations))
    }

    override def transformSchema(schema: StructType): StructType = validateAndTransformSchema(schema)

    override def write: MLWriter = new LinearRegression.LinearRegressionWriter(this)
}


object LinearRegression extends DefaultParamsReadable[LinearRegression] with MLReadable[LinearRegression] {

    override def read: MLReader[LinearRegression] = new LinearRegressionReader

    override def load(path: String): LinearRegression = super.load(path)

    private class LinearRegressionWriter(instance: LinearRegression) extends MLWriter {

        override protected def saveImpl(path: String): Unit = {
            DefaultParamsWriter.saveMetadata(instance, path, sc)

            val stepSize = instance.stepSize
            val numIterations = instance.numIterations
            val data = Data(stepSize, numIterations)
            sparkSession
                .createDataFrame(Seq(data))
                .repartition(1)
                .write
                .parquet(path + "/data")
        }

        private case class Data(stepSize: Double, numIterations: Int)
    }

    private class LinearRegressionReader extends MLReader[LinearRegression] {
        private val className = classOf[LinearRegression].getName

        override def load(path: String): LinearRegression = {
            val metadata = DefaultParamsReader.loadMetadata(path, sc, className)

            val data = sparkSession
                .read
                .parquet(path + "/data")
                .select("stepSize", "numIterations")
                .head()
            val stepSize = data.getAs[Double](0)
            val numIterations = data.getAs[Int](1)

            val estimator = new LinearRegression(metadata.uid, stepSize, numIterations)
            metadata.getAndSetParams(estimator)
            estimator.optimizer.setInputCol(estimator.getInputCol)
            estimator.optimizer.setOutputCol(estimator.getOutputCol)

            estimator
        }
    }
}


class LinearRegressionModel private[made](override val uid: String,
                                          val weights: DenseVector,
                                          val bias: Double)
    extends Model[LinearRegressionModel]
        with LinearRegressionParams
        with MLWritable {

    override def copy(extra: ParamMap): LinearRegressionModel = {
        copyValues(new LinearRegressionModel(weights, bias))
    }

    private[made] def this(weights: DenseVector, bias: Double) =
        this(Identifiable.randomUID("LinearRegressionModel"), weights, bias)

    override def transform(dataset: Dataset[_]): DataFrame = {
        val transformUdf = dataset.sqlContext.udf.register(uid + "_transform",
            (x: Vector) => {
                sum(x.asBreeze *:* weights.asBreeze) + bias
            })

        dataset.withColumn($(outputCol), transformUdf(dataset($(inputCol))))
    }

    override def transformSchema(schema: StructType): StructType = validateAndTransformSchema(schema)

    override def write: MLWriter = new LinearRegressionModel.LinearRegressionModelWriter(this)
}


object LinearRegressionModel extends DefaultParamsReadable[LinearRegressionModel] with MLReadable[LinearRegressionModel] {
    override def read: MLReader[LinearRegressionModel] = new LinearRegressionModelReader

    override def load(path: String): LinearRegressionModel = super.load(path)

    private class LinearRegressionModelWriter(instance: LinearRegressionModel) extends MLWriter {

        override protected def saveImpl(path: String): Unit = {
            DefaultParamsWriter.saveMetadata(instance, path, sc)

            val weights = instance.weights
            val bias = instance.bias
            val data = Data(weights, bias)
            sparkSession
                .createDataFrame(Seq(data))
                .repartition(1)
                .write
                .parquet(path +  "/data")
        }

        private case class Data(weights: DenseVector, bias: Double)
    }

    private class LinearRegressionModelReader extends MLReader[LinearRegressionModel] {

        private val className = classOf[LinearRegressionModel].getName

        override def load(path: String): LinearRegressionModel = {
            val metadata = DefaultParamsReader.loadMetadata(path, sc, className)

            val data = sparkSession
                .read
                .parquet(path + "/data")
                .select("weights", "bias")
                .head()
            val weights = data.getAs[DenseVector](0)
            val bias = data.getAs[Double](1)

            val model = new LinearRegressionModel(metadata.uid, weights, bias)
            metadata.getAndSetParams(model)

            model
        }
    }
}
