--- Action (5): Write into hbase:
-- on hbase shell: create 'andresnz_nyt_hbase', 'stats'
/*
create table andresnz_nyt_hbase (
  decade_topic string,
  decade string,
  topic string,
  wordcount bigint,
  ttalwords bigint,
  value bigint)
  STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,stats:decade,stats:topic,stats:wordcount#b,stats:ttalwords#b,stats:value#b')
TBLPROPERTIES ('hbase.table.name' = 'andresnz_nyt_hbase');

insert overwrite table andresnz_nyt_hbase
select concat(topic, decade), decade, topic, wordcount, ttalwords, value
from andresnz_nyt_topics;
*/
