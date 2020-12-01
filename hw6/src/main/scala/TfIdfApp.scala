import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.storage.StorageLevel

object TfIdfApp {
    def main(args: Array[String]): Unit = {
        // Get Spark session
        val spark = SparkSession.builder()
            .master("local[*]")
            .appName("made-tfidf")
            .getOrCreate()

        // Read CSV file and add column with review id
        val Reviews = spark.read
            .option("header", "true")
            .option("inferSchema", "true")
            .csv("data/tripadvisor_hotel_reviews.csv")
            .withColumn("ReviewId", monotonically_increasing_id())
//            .limit(1000)

        // Clean "Review" column and split it by whitespace
        val cleanReviews = Reviews
            .select(col("Review"), col("ReviewId"))
            .withColumn("Review", lower(col("Review")))
            .withColumn("Review", regexp_replace(col("Review"), """[^\w\s]+""", ""))
            .withColumn("Review", regexp_replace(col("Review"), """\s+""", " "))
            .withColumn("Review", trim(col("Review")))
            .withColumn("Review", split(col("Review"), pattern = " "))

        // Add total words in document count column
        val cleanReviewsWithCount = cleanReviews
            .withColumn("ReviewWordCount", size(col("Review")))

        // Yield a row for each word in each review
        val ExplodedWords = cleanReviewsWithCount
            .withColumn("Word", explode(col("Review")))

        // Calculate TF
        val ReviewWordTF = ExplodedWords
            .groupBy("ReviewId", "Word")
            .agg(
                count("Review") as "TF",
                first("ReviewWordCount") as "ReviewWordCount")
            .withColumn("TF", col("TF") / col("ReviewWordCount"))

        // Calculate DF and select only top100 popular
        val w = Window.orderBy(col("DF").desc)
        val WordDF = ExplodedWords
            .groupBy("Word")
            .agg(countDistinct("ReviewId") as "DF")
            .withColumn("rn", row_number.over(w))
            .where(col("rn") < 100)
            .drop("rn")

        // Calculate IDF
        val ReviewCount = Reviews.count()
        val calcIDF = udf { df: Long => math.log(ReviewCount.toDouble / (df.toDouble + 0.01)) }
        val WordIDF = WordDF
            .withColumn("IDF", calcIDF(col("DF")))

        // Calculate TF-IDF
        val ReviewWordTFIDF = WordIDF
            .join(ReviewWordTF, Seq("Word"), "left")
            .withColumn("TF_IDF", col("TF") * col("IDF"))

        // Make a pivot table
        val TfIdfPivot = ReviewWordTFIDF.groupBy("ReviewId")
            .pivot(col("Word"))
            .agg(first(col("TF_IDF"), ignoreNulls = true))
            .na.fill(0.0)

        TfIdfPivot.persist(StorageLevel.MEMORY_ONLY)

        TfIdfPivot.show(20)

        // Save table onto disk
        TfIdfPivot
            .coalesce(1)
            .write
            .option("header", "true")
            .option("sep", ",")
            .csv("data/result.csv")
    }
}
