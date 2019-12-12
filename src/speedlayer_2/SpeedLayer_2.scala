/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package edu.uchicago.andresnz.wcspeed

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

import org.apache.spark.ml.feature.StopWordsRemover

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Adapted from the Spark Streaming Examples
 * Usage: spark-submit ... <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 */
object SparkWordCount {
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
    val sparkConf = new SparkConf().setAppName("DirectKafkaWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    // Create direct kafka stream with brokers and topics
    val topicsSet = Set("andresnz_speed")
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    // process data as an RDD
    // val lines = sc.textFile("/tmp/andresnz/98-0.txt") note

    val lines = messages.map(_._2)
    // val words = lines.flatMap(_.split(" "))
    val words = lines.flatMap(_.split("\\s")
    ).map(_.replaceAll("[,.!?:;]", ""
    ).trim.toLowerCase).filter(!_.isEmpty)
    val wordCounts = words.map(x => (Seq(x), 1L)).reduceByKey(_ + _)
    //wordCounts.print()

    // process data with dataframes
    val df = spark.createDataFrame(wordCounts).toDF("raw_texts", "wordcount")
    val remover = new StopWordsRemover(
                                  ).setInputCol("raw_texts"
                                  ).setOutputCol("texts")
    val df_2 = remover.transform(df).drop($"raw_texts").withColumn("word", explode($"texts")).drop($"texts")

    val ttal_words = df_2.agg(sum("wordcount").as("ttalwords")).na.fill(0, Seq("ttalwords")).first().getLong(0)
    val topic_clim = "climate"
    val topic_imm = "immigration"
    val topic_other = "other"
    val decade = "2011 - 2020"
    val wordcount_clim = df_2.filter(col("word").contains(topic_clim)).agg(sum("wordcount")).na.fill(0, Seq("sum(wordcount)")).first().getLong(0)
    val wordcount_imm = df_2.filter(col("word").contains("immigra")).agg(sum("wordcount")).na.fill(0, Seq("sum(wordcount)")).first().getLong(0)
    val wordcount_other = df_2.filter(!col("word").contains(topic_imm)).filter(!col("word").contains(topic_clim)).agg(sum("wordcount")).na.fill(0, Seq("sum(wordcount)")).first().getLong(0)


    // connect with hbase
    val hbaseConf: Configuration = HBaseConfiguration.create()
  	hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")
  	hbaseConf.set("hbase.zookeeper.quorum","mpcs53014c10-m-6-20191016152730.us-central1-a.c.mpcs53014-2019.internal")
  	hbaseConf.set("zookeeper.znode.parent", "/hbase-unsecure")
    val hbaseConnection = ConnectionFactory.createConnection(hbaseConf)

    val andresnzSpeed = hbaseConnection.getTable(TableName.valueOf("andresnz_nyt_speed"))

    // update speed table
    // climate
    val inc = new Increment(Bytes.toBytes(topic_clim + decade))
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("ttalwords"), ttal_words)
    andresnzSpeed.increment(inc)
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("wordcount"), wordcount_clim)
    andresnzSpeed.increment(inc)
    // immigration
    val inc = new Increment(Bytes.toBytes(topic_imm + decade))
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("ttalwords"), ttal_words)
    andresnzSpeed.increment(inc)
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("wordcount"), wordcount_imm)
    andresnzSpeed.increment(inc)
    // other
    val inc = new Increment(Bytes.toBytes(topic_other + decade))
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("ttalwords"), ttal_words)
    andresnzSpeed.increment(inc)
    inc.addColumn(Bytes.toBytes("stats"), Bytes.toBytes("wordcount"), wordcount_other)
    andresnzSpeed.increment(inc)

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
