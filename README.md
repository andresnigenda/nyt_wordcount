# Exploring the occurence of relevant topics on New York Times (NYT) headlines
Web app developped for Mike Spertus' UChicago MPCS Big Data Applications class.

## Context
This web app uses data from the NYT [archive API](https://developer.nytimes.com/docs/archive-product/1/overview), the [National Oceanic and Atmospheric Administration](https://www.ncdc.noaa.gov/cag/national/time-series/110-tavg-all-1-1901-2019.csv?base_prd=true)(NOAA) and the [US Department of Homeland Security](https://www.dhs.gov/immigration-statistics/refugees-asylees)(DHS) to analyze how certain relevant topics like climate change and immigration have been covered in NYT headlines by decade, and whether this coverage follows observed trends in tangible data (e.g. average atmospheric temperature per decade, average number of new legal immigrants per decade).

## How this app is a Big Data app
- Volume: since this app lives in a cluster, it is able to efficiently handle and process data for posterior querying on NYT headlines from 1901 to 2019. For the sake of space in the class cluster, I limited the timeframe from 1971 to 2019 (50 years).
- Velocity: even though there isn't data coming in "fast and furious" (there's just so many articles published per day), the big data stack allows to process new data and recompute views to populate the tables with which the user interacts with
- Variety: this app takes data from three different data sources and joins them all together, this would have been very messy to handle in a conventional relational database.
Overall this app is scalable: many more years of data can be added, many more topics and related context data can also be added, and it could receive updates from different APIs to update its views.

## Data preparation
First, I downloaded all article data from the NYT by making calls every six seconds (set limit to not be blocked) to the archive API, looping through all months and years from 1901 to 2019, I dropped some irrelevant features to occupy less space on the cluster and I put the files into HDFS as ```hdfs dfs -put /inputs/andresnz_nyt```. This job was done on Python. For the NOAA and DHS data I made direct calls on the cluster ``` wget -0 file_name.csv address``` and then put into HDFS in the ```/inputs/andresnz_temperature``` and ```/inputs/andresnz_immigration``` respectively. Then, I put all data tables into hive (see data_tohive.hql). 

I did all of my processing (word count, cleaning, joining) on scala on the spark shell (see details in data_processing.scala). Overall my data processing consisted of:
1. A word count on NYT article headlines, transforming to lower case, stripping punctuation and removing stop words (with the spark.ml.StopWordsRemover)
2. Creating a "decade" variable to join all of my tables
3. Three main dataframe style joins on values aggregated by decade appeared, and also a count of total words by decade
I wrote my data into hive as ```andresnz_nyt_topics```.

Finally, I moved my hive table into hbase (see data_tohbase.hql).

## Web app
Fo the web app, I present an interface in which the user can query for the occurence of certain topics on NYT headlines. Each topic shows the topic as word counts and as a percentage of all words in that decade to see how its coverage has evolved along the decades. I also show the evolutio on the same time frames of a relevant tangible variable that is associated with it: average temperature per decade fro climate change and average number of immigrants per decade for immigration. I only show two topics as I had to look for relevant data for each topic and then transform and join the data. Some other relevant topics that I wanted to explore, for example, were abortion, homosexuality, democracy.

The app is built in node js and is in the webserver and the website lives in http://34.66.189.234:3889/nyt-topics.html. It can be launched as ```node app.js``` on ```/home/andresnz/andresnz_nyt_app``` in the webserver.

## Speed layer
I have two versions of the speed layer. The one that is built with the app and on the webserver is a simple user interface that lives in http://34.66.189.234:3889/submit-counts.html and that allows to show a functioning speed layer. The user interface asks as an input the word count of a title the climate and immigration topics as well as the rest of the words. Admittedly, this version of the speed layer is not well suited for a production app implementation since it asks for the user to input in word counts. However, it shows how the word counts are updated on hbase and then the webapp is updated (see app


## File structure
This repository's structure is as follows:

```
.
├── README.md                         
├── requirements.txt                 
└── src/                             # Contains all code and outputs
    ├── content_analysis.ipynb       # Analysis and results notebook, generic version available
    ├── data_detail.csv              # Metadata for obtained url set
    ├── images/                      # Contains image .png files with corresponding .csv file
    ├── inputs/                      # Contains input and intermediate files
    │    ├── departments_final.csv            # Department names
    │    ├── final_urls_for_visual_check.csv  # URLs for visual check (intermediate)
    │    ├── hhslinks_final.csv               # Final links for hhs analysis (from wayback machine)    
    │    ├── links_final.csv                  # Final links for content analysis  
    │    ├── usagovsearch_urls.csv            # Queries to get second set of URLs (intermediate)
    │    └── wip_identified.csv               # First set of WIP identified URLs  
    └── scripts/                     # Contains all code for this project
         ├── analysis.py                      # Main analysis functions
         ├── chromedriver                     # Driver for webscraping
         ├── get_content.py                   # Content extraction functions
         ├── internetarchive.py               # EDGI module
         ├── sentiment_analysis.py            # Sentiment analysis functions
         └── utils.py                         # EDGI module
```

## Requirements
- Python 3.x
- For the sentiment analysis you will need to make sure that the `pysentiment`
package is correctly installed:

  `pip install git+https://github.com/hanzhichao2000/pysentiment`

  If the [static folder](https://github.com/hanzhichao2000/pysentiment/tree/master/pysentiment/static) does not download, you will need to download it and then
  copy it to your python packages `pysentiment`folder, which should look like this:

  `cp -R STATIC YOUR_DOWNLOAD_PATH '/usr/local/lib/python3.7/site-packages/pysentiment'`
- To run `selenium` you will need to make sure that your Chrome Browser's version (which you can check on your browser's menu: `Chrome > About Chrome`) matches the provided ChromeDriver version (76.0.3809.126). You can download and substitute the driver with the latest [version](https://sites.google.com/a/chromium.org/chromedriver/downloads).
