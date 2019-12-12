-- Action (0): Put file into HDFS
--             hdfs dfs -put nyt_data.csv /inputs/andresnz
-- Action (1): Map csv article data in Hive
-- Note:
--      ALTER TABLE nyt_articles SET TBLPROPERTIES('EXTERNAL'='False');
--      drop table nyt_articles;

create external table andresnz_nyt_7119(
  article_id string,
  date_timestamp string,
  title string)
  row format serde 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
     "separatorChar" = "\,",
     "quoteChar"     = "\""
  )
  STORED AS TEXTFILE
    location '/inputs/andresnz_nyt';

--- weather change
-- wget -O noaa_temp_data.csv https://www.ncdc.noaa.gov/cag/national/time-series/110-tavg-all-1-1901-2019.csv?base_prd=true

create external table andresnz_temperature(
  date_ bigint,
  value float,
  anomaly float)
  row format serde 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
     "separatorChar" = "\,",
     "quoteChar"     = "\""
  )
STORED AS TEXTFILE
    location '/inputs/andresnz_temperature'
TBLPROPERTIES ("skip.header.line.count"="5")
    ;

--- immigration
create external table andresnz_immigration(
  year bigint,
  total bigint)
  row format serde 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
     "separatorChar" = "\,",
     "quoteChar"     = "\""
  )
STORED AS TEXTFILE
    location '/inputs/andresnz_immigration'
    ;


-- Action (2): Test query to check that table exists
select title from andresnz_nyt_7119 limit 5;
select * from andresnz_temperature limit 5;