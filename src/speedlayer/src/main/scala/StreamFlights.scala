import kafka.serializer.StringDecoder
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Increment
import org.apache.hadoop.hbase.util.Bytes

object StreamCounts {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  val hbaseConf: Configuration = HBaseConfiguration.create()
  hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")

  // Use the following two lines if you are building for the cluster
  hbaseConf.set("hbase.zookeeper.quorum","mpcs53014c10-m-6-20191016152730.us-central1-a.c.mpcs53014-2019.internal")
  hbaseConf.set("zookeeper.znode.parent", "/hbase-unsecure")

  val hbaseConnection = ConnectionFactory.createConnection(hbaseConf)
  val hbase_nyt = hbaseConnection.getTable(TableName.valueOf("andresnz_nyt_hbase"))

  def incrementCount(rr : CountReport) : String = {
	
	val ttalWords = rr.climate + rr.immigration + rr.other
    // Climate
    val rowKey = "climate2011 - 2020"
    val inc = new Increment(Bytes.toBytes(rowKey))
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("wordcount"), rr.climate)
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("ttalwords"), ttalWords)
    hbase_nyt.increment(inc)
    // Immigration
    val rowKey2 = "immigration2011 - 2020"
    val inc2 = new Increment(Bytes.toBytes(rowKey2))
    inc2.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("wordcount"), rr.immigration)
    inc2.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("ttalwords"), ttalWords)
    hbase_nyt.increment(inc2)
    // Other
    val rowKey3 = "other2011 - 2020"
    val inc3 = new Increment(Bytes.toBytes(rowKey3))
    inc3.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("wordcount"), rr.other)
    inc3.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("ttalwords"), ttalWords)
    hbase_nyt.increment(inc3)

    return "Updated speed layer for headline"
}

  def main(args: Array[String]) {
    if (args.length < 1) {
      System.err.println(s"""
        |Usage: StreamFlights <brokers>
        |  <brokers> is a list of one or more Kafka brokers
        |
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers) = args

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("StreamCounts")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    // Create direct kafka stream with brokers and topics
    // andresnz_speed (old)
    val topicsSet = Set("andresnz_kafka_wc")
    // Create direct kafka stream with brokers and topics
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    // Get the lines, split them into words, count the words and print
    val serializedRecords = messages.map(_._2);

    val kfrs = serializedRecords.map(rec => mapper.readValue(rec, classOf[CountReport]))

    // Update speed table
    val processedCounts = kfrs.map(incrementCount)
    processedCounts.print()
    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }

}
