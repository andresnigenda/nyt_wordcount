/// Action (0): Process data
/// On the spark shell: spark-shell --conf spark.hadoop.metastore.catalog.default=hive
/// inspired by: http://geoinsyssoft.com/2016/08/24/spark-dataframe-using-hive-table/

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.sql.SaveMode

/// nyt data
val df = spark.table("andresnz_nyt_7119")
val df_ = df.filter("title is not null"
           ).withColumn("year", expr("substr(date_timestamp, 1, 4)") cast "Int")
val remover = new StopWordsRemover(
                                  ).setInputCol("word"
                                  ).setOutputCol("texts")
val df_years = df_.groupBy("year"
                              ).agg(concat_ws(" ", collect_list("title")) as "raw_texts"
                              ).withColumn("raw_texts",
                              split(lower(regexp_replace(col("raw_texts"),
                                    """[\p{Punct}]""", "")), "\\s+"
                              ))
val df_temp = remover.transform(df_years).drop($"raw_texts"
             ).withColumn("word", explode($"texts")).groupBy("year", "word").count
val df_decades = df_temp.filter("year is not null"
                       ).withColumn("stamp", $"year" cast "string"
                       ).withColumn("b_last", expr("substr(stamp, 3, 1)") cast "Int"
                       ).withColumn("2_last", expr("substr(stamp, 3, 2)") cast "Int"
                       ).withColumn("b_last_minus1", $"b_last" - 1
                       ).withColumn("b_last_plus1", $"b_last" + 1
                       ).withColumn("last", expr("substr(stamp, 4, 1)") cast "Int"
                       ).withColumn("b_last_plus1", $"b_last_plus1" cast "string"
                       ).withColumn("b_last_minus1", $"b_last_minus1" cast "string"
                       ).withColumn("b_last", $"b_last" cast "string"
                       )
val df_temp_2 = df_decades.withColumn("decade", concat(lit(when($"year" =!= 2000, expr("substr(stamp, 1, 2)")).otherwise("19"))
                                                   , lit(when((df_decades("last") === 0).and($"year" =!= 2000), $"b_last_minus1").otherwise(when($"year" =!= 2000, $"b_last").otherwise("9")))
                                                   , lit("1 - ")
                                                   , lit(when($"year" > 1990, "20").otherwise("19"))
                                                   , lit(when((df_decades("2_last") > 90).and($"2_last" <= 99), "0").otherwise(when($"last" === 0, $"b_last").otherwise($"b_last_plus1")))
                                                   , lit("0"))
                       )

val nyt_wc_word = df_temp_2.drop($"2_last").drop($"b_last").drop($"b_last_plus1").drop($"last").drop($"b_last_minus1").groupBy("decade", "word").count
val nyt_wc_decade = nyt_wc_word.groupBy($"decade").agg(count("*").as("ttalwords"))

/// weather data

val lisst = List("null", "Anomaly")
val df_weather = spark.table("andresnz_temperature").filter(!col("anomaly").isin(lisst:_*))
val weather_decades = df_weather.withColumn("year", $"date_" cast "string"
                               ).withColumn("year", expr("substr(year, 1, 4)") cast "Int"
                               ).filter("year is not null"
                               ).drop($"date_"
                               ).filter($"year" >= 1971)
val df_decades_w = weather_decades.withColumn("stamp", $"year" cast "string"
                      ).withColumn("b_last", expr("substr(stamp, 3, 1)") cast "Int"
                      ).withColumn("2_last", expr("substr(stamp, 3, 2)") cast "Int"
                      ).withColumn("b_last_minus1", $"b_last" - 1
                      ).withColumn("b_last_plus1", $"b_last" + 1
                      ).withColumn("last", expr("substr(stamp, 4, 1)") cast "Int"
                      ).withColumn("b_last_plus1", $"b_last_plus1" cast "string"
                      ).withColumn("b_last_minus1", $"b_last_minus1" cast "string"
                      ).withColumn("b_last", $"b_last" cast "string"
                      )
val df_temp_w = df_decades_w.withColumn("decade", concat(lit(when($"year" =!= 2000, expr("substr(stamp, 1, 2)")).otherwise("19"))
                                                   , lit(when((df_decades_w("last") === 0).and($"year" =!= 2000), $"b_last_minus1").otherwise(when($"year" =!= 2000, $"b_last").otherwise("9")))
                                                   , lit("1 - ")
                                                   , lit(when($"year" > 1990, "20").otherwise("19"))
                                                   , lit(when((df_decades_w("2_last") > 90).and($"2_last" <= 99), "0").otherwise(when($"last" === 0, $"b_last").otherwise($"b_last_plus1")))
                                                   , lit("0"))
                       )
val weather_dec = df_temp_w.drop($"2_last").drop($"b_last").drop($"b_last_plus1").drop($"last").drop($"b_last_minus1").groupBy("decade").agg(sum("value")).withColumn("Temperature", $"sum(value)" cast "Int").drop($"sum(value)")

/// immigration data

val df_immigration = spark.table("andresnz_immigration")
val immigration_decades = df_immigration.withColumn("total", regexp_replace(col("total"), "\\,", "") cast "Int"
                               ).withColumn("year", $"year" cast "string"
                               ).withColumn("year", expr("substr(year, 1, 4)") cast "Int"
                               ).filter("year is not null"
                               ).drop($"date_"
                               ).filter($"year" >= 1971)
val df_decades_i = immigration_decades.withColumn("stamp", $"year" cast "string"
                      ).withColumn("b_last", expr("substr(stamp, 3, 1)") cast "Int"
                      ).withColumn("2_last", expr("substr(stamp, 3, 2)") cast "Int"
                      ).withColumn("b_last_minus1", $"b_last" - 1
                      ).withColumn("b_last_plus1", $"b_last" + 1
                      ).withColumn("last", expr("substr(stamp, 4, 1)") cast "Int"
                      ).withColumn("b_last_plus1", $"b_last_plus1" cast "string"
                      ).withColumn("b_last_minus1", $"b_last_minus1" cast "string"
                      ).withColumn("b_last", $"b_last" cast "string"
                      )
val df_temp_i = df_decades_i.withColumn("decade", concat(lit(when($"year" =!= 2000, expr("substr(stamp, 1, 2)")).otherwise("19"))
                                                   , lit(when((df_decades_i("last") === 0).and($"year" =!= 2000), $"b_last_minus1").otherwise(when($"year" =!= 2000, $"b_last").otherwise("9")))
                                                   , lit("1 - ")
                                                   , lit(when($"year" > 1990, "20").otherwise("19"))
                                                   , lit(when((df_decades_i("2_last") > 90).and($"2_last" <= 99), "0").otherwise(when($"last" === 0, $"b_last").otherwise($"b_last_plus1")))
                                                   , lit("0"))
                       )
val immigration_dec = df_temp_i.drop($"2_last").drop($"b_last").drop($"b_last_plus1").drop($"last").drop($"b_last_minus1").groupBy("decade").agg(sum("total")).withColumnRenamed("sum(total)", "Immigrants")


/// term search on topics: climate change, immigration
val nyt_by_topic = nyt_wc_word.withColumn("topic", when((nyt_wc_word("word").contains("climate")), "climate").otherwise(when(nyt_wc_word("word").contains("migrant") || nyt_wc_word("word").contains("immigration"), "immigration").otherwise("other")))
val nyt_topic_collapse = nyt_by_topic.groupBy("decade", "topic").agg(sum("count").as("wordcount"))

/// join wordcount with total words by decade with topic specific data
val nyt_topics = nyt_topic_collapse.join(nyt_wc_decade, Seq("decade"), "left")
val andresnz_nyt_topics = nyt_topics.join(weather_dec, Seq("decade"), "left"
).join(immigration_dec, Seq("decade"), "left").withColumnRenamed("decade", "Decade"
).withColumn("Value", when($"topic" === "climate", $"Temperature"
).otherwise($"Immigrants")).drop($"Immigrants").drop($"Temperature")

andresnz_nyt_topics.write.mode(SaveMode.Overwrite).saveAsTable("andresnz_nyt_topics")
