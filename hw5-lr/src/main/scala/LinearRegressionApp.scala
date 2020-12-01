import java.io.File

import breeze.linalg.{DenseMatrix, DenseVector, csvread, csvwrite}
import normalizers.StandardScaler
import optimizers.SGD
import models.LinearRegression
import metrics.{MSE, R2}


object LinearRegressionApp {
    def main(args: Array[String]): Unit = {
        // Need at least 2 arguments (train file and test files)
        if (args.length < 2) {
            println("Two files must be provided - train data and test data")
            return -1
        }

        // Get train and test files
        val trainFile = new File(args(0))
        val testFile = new File(args(1))

        // Read train data
        val trainData = csvread(trainFile, separator = ',', skipLines = 1)
        var (trainX, trainY) = (trainData(::, 0 until trainData.cols - 1), trainData(::, -1))

        // Read test data
        val testData = csvread(testFile, separator = ',', skipLines = 1)
        var (testX, testY) = (testData(::, 0 until trainData.cols - 1), testData(::, -1))

        // Normalize train data
        val target_scaler = StandardScaler()
        trainY = target_scaler.fit_transform(trainY)
        var train_scalers: Array[StandardScaler] = Array()
        for (i <- 0 until trainX.cols) {
            val scaler = StandardScaler()
            scaler.fit(trainX(::, i))
            trainX(::, i) := scaler.transform(trainX(::, i))
            train_scalers :+= scaler
        }

        // Normalize test data
        for (i <- 0 until testX.cols) {
            testX(::, i) := train_scalers(i).transform(testX(::, i))
        }

        val opt = SGD(learning_rate = 1e-3, batch_size = 1000)

        val lr = LinearRegression()
        lr.fit(trainX, trainY, optimizer = opt, epochs = 500)

        var y_pred = lr.predict(testX)
        y_pred = target_scaler.reverse_transform(y_pred)

        println(String.format("R2 = %f", R2(testY, y_pred)))
        println(String.format("MSE = %f", MSE(testY, y_pred)))

        val predictions: DenseMatrix[Double] = DenseMatrix.zeros[Double](y_pred.length, 1)
        predictions(::, 0) := y_pred
        csvwrite(new File("data/predictions.csv"), predictions)
    }
}
