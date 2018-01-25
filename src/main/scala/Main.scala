import java.io.{BufferedOutputStream, File, FileOutputStream, ObjectOutputStream}

import org.apache.spark.sql.{Dataset, Encoders, SparkSession}
import io.github.nwtgck.wikipedia_dump_loader.{Page, Redirect, Revision, WikipediaDumpLoader}

object Main {
  def main(args: Array[String]): Unit = {
    require(args.length == 1)

    // Get Wikipedia Dump XML Path
    val wikipediaPath: String = args(0)

    // Create spark session
    val sparkSession: SparkSession = SparkSession
      .builder()
      .appName("Wikipedia Dump Loader Test [Spark session]")
      .master("local[*]")
      .config("spark.executor.memory", "1g")
      .getOrCreate()

    // Import implicits
    import sparkSession.implicits._

    // Parquet Path
    val parquetPath: String = {
      val xmlFileName: String = new File(wikipediaPath).getName
      s"out/${xmlFileName}.ds.parquet"
    }

    // Get scheme of Page
    val pageScheme = Encoders.product[Page].schema

    // Get Page Dataset
    val pageDs: Dataset[Page] = if(new File(parquetPath).exists()){

      println(s"Load existing '${parquetPath}'")

      // Load page Dataset from Parquet
      val pageDs: Dataset[Page] =
        sparkSession
          .read
          .schema(pageScheme)
          .parquet(parquetPath)
          .as[Page]
      pageDs
    } else {
      // Load page Dataset from Wikipedia Dump XML
      val pageDs: Dataset[Page] = WikipediaDumpLoader.readXmlFilePath(
        sparkSession,
        filePath = wikipediaPath
      )
      // Write the Dataset to output
      pageDs
        .write
        .parquet(parquetPath)
      pageDs
    }

    println(s"Page length: ${pageDs.count()}")

  }
}
