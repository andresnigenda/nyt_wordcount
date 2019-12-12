# Exploring the occurence of relevant topics on New York Times (NYT) headlines
Web app developped for Mike Spertus' UChicago MPCS Big Data Applications class.

## Context
This web app uses data from the NYT [archive API](https://developer.nytimes.com/docs/archive-product/1/overview), the [National Oceanic and Atmospheric Administration](https://www.ncdc.noaa.gov/cag/national/time-series/110-tavg-all-1-1901-2019.csv?base_prd=true)(NOAA) and the [US Department of Homeland Security](https://www.dhs.gov/immigration-statistics/refugees-asylees)(DHS) to analyze how certain relevant topics like climate change and immigration have been covered in NYT headlines by decade, and whether this coverage follows observed trends in tangible data (e.g. average atmospheric temperature per decade, average number of new legal immigrants per decade).

A snippet of the fully functioning app with a speed layer can be seen on (webapplq.mov)

## Description of the project
### How this app is a Big Data app
- Volume: since this app lives in a cluster, it is able to efficiently handle and process data for posterior querying on NYT headlines from 1901 to 2019. For the sake of space in the class cluster, I limited the timeframe from 1971 to 2019 (50 years).
- Velocity: even though there isn't data coming in "fast and furious" (there's just so many articles published per day), the big data stack allows to process new data and recompute views to populate the tables with which the user interacts with
- Variety: this app takes data from three different data sources and joins them all together, this would have been very messy to handle in a conventional relational database.
Overall this app is scalable: many more years of data can be added, many more topics and related context data can also be added, and it could receive updates from different APIs to update its views.

### Batch layer
First, I downloaded all article data from the NYT by making calls every six seconds (set limit to not be blocked) to the archive API, looping through all months and years from 1901 to 2019, I dropped some irrelevant features to occupy less space on the cluster and I put the files into HDFS as ```hdfs dfs -put /inputs/andresnz_nyt```; this job was done on Python. For the NOAA and DHS data I made direct calls on the cluster ``` wget -0 file_name.csv address``` and then put into HDFS in the ```/inputs/andresnz_temperature``` and ```/inputs/andresnz_immigration``` respectively. Then, I put all data tables into hive (see data_tohive.hql). 

I did all of my processing (word count, cleaning, joining) on scala on the spark shell (see details in data_processing.scala). Overall my data processing consisted of:
1. A word count on NYT article headlines, transforming to lower case, stripping punctuation and removing stop words (with the spark.ml.StopWordsRemover)
2. Creating a "decade" variable to join all of my tables
3. Three main dataframe style joins on values aggregated by decade appeared, and also a count of total words by decade
I wrote my data into hive as ```andresnz_nyt_topics```.

### Serving Layer
Finally, I moved my hive table into hbase (see data_tohbase.hql).

### Speed layer
I have two versions of the speed layer. The one that is built with the app and on the webserver is a simple user interface that lives in http://34.66.189.234:3889/submit-counts.html and that allows to show a functioning speed layer. The user interface asks as an input the word count of a title the climate and immigration topics as well as the rest of the words. Admittedly, this version of the speed layer is not well suited for a production app implementation since it asks for the user to input in word counts. However, it shows how the word counts are updated on hbase and then the webapp is updated (see webapp.mov), and thus demonstrates a functioning serving layer. The video shows how if we update the word counts as {climate: 10, immigration: 10, other: 10}, hbase will be updated such that the web app displays  an increase of 10 words for the "Word Count" column on the immigration topic as well as an increase of 30 words in the "Total Words" column. The app updates the "immigration" and "other" topic with the same logic, as the three topics are complements.

My second version of the speed layer takes in a title via an interface and automatically performs the counts on the text. However, I ran out of time to fully implement the app (namely pulling individuals RDDs from a DStream object). The code for this unfinished version of my speed layer is under (speedlayer_title.scala and submit-title.html).

### Web app
For the web app, I present an interface in which the user can query for the occurence of certain topics on NYT headlines. Each topic shows the topic as word counts and as a percentage of all words in that decade to see how its coverage has evolved along the decades. I also show the evolutio on the same time frames of a relevant tangible variable that is associated with it: average temperature per decade fro climate change and average number of immigrants per decade for immigration. I only show two topics as I had to look for relevant data for each topic and then transform and join the data. Some other relevant topics that I wanted to explore, for example, were abortion, homosexuality, democracy.

The app is built in node js and is in the webserver and the website lives in http://34.66.189.234:3889/nyt-topics.html. It can be launched as ```node app.js``` on ```/home/andresnz/andresnz_nyt_app``` in the webserver.

## Caveats (to dos)
Even though this project shows a functioning app following a lambda architecture, there is still great room for scalability and improvement. On the scalability side of things, more data (all years starting from 1901) and topics can be added to the batch layer. On the improvement side, the speed layer should automatically parse titles and update hbase (and could even make calls to a NYT API), the topics could be further defined to include more terms that define a given topic (e.g. include refugee in the immigration topic), and better related variables could be shown (e.g. number of record temperature setting days per year for climate change).

### Repository structure
This repository's structure is as follows:

```
.
├── README.md                         
├── requirements.txt 
├── webapp_lq.mov                    # Conatins a video of the app running
└── src/                             # Contains all code and outputs
    ├── app/                         # Contains the files for the webapp (in cluster: andresnz@webserver:/home/andresnz/andresnz_nyt_app)
    ├── data/                        # Contains a sample of the data (NYT article headlines from 2011 to 2019)
    ├── data_processing.scala        # Scala script to clean, transform and join the 3 data sources
    ├── data_tohbase.hql             # Script to put table into hbase (from hive)
    ├── data_tohive.hql              # Script to put 3 data sources into hive tables
    ├── speedlayer/                  # Contains maven project with uberjar for the speed layer
    │    ├── target/                 # Contains scripts to ingest data from a kafka topic into hbase
    ├── get_nyt_data.ipynb           # Python code to get data from NYT Archive API
    └── speedlayer2/                 # Contains scala script for second version of speed layer + text submit form
```
