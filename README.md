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
First, I downloaded all article data from the NYT by making calls every six seconds (set limit to not be blocked) to the archive API, looping through all months and years from 1901 to 2019, I dropped some irrelevant features to occupy less space on the cluster and I put the files into HDFS as ```hdfs dfs -put /inputs/andresnz_nyt```. This job was done on Python. For the NOAA and DHS data I made direct calls on the cluster ``` wget -0 file_name.csv address``` and then put into HDFS in the ```/inputs/andresnz_temperature``` and ``/inputs/andresnz_immigration``` respectively.


## Background
These scripts were used in the analysis for Sunlight Foundation's [Identity, Protections, and Data Coverage: How LGBTQ-related language and content has changed under the Trump Administration](https://sunlightfoundation.com/web-integrity-project/), which looks at how the use of LGBTQ-related terms has changed between the Obama and Trump administrations across federal department websites.

## Methodology
We pooled together a set of WIP-identified URLs (1) with a set of URLs coming
from a search of specific terms on the usa.gov search engine (2).
For each URL, these scripts fetch the latest available IAWM "snapshot" for the "pre" inauguration and "post" inauguration period and count the number of times that a
set of terms appear in the "visible text" of the webpage.

To analyze the text from our set of web pages these scripts adapt [EDGI’s
code](https://github.com/ericnost/EDGI) and extend its functionality to
support sentiment analysis that is tailored to the set of analyzed URLs.
To compare websites before and after the 2017 inauguration, we rely
on websites archived on the Internet Archive’s Wayback Machine [IAWM](https://archive.org/web/) website archives.

Read more about the methodology [here](https://sunlightfoundation.com/web-integrity-project/)

## Results
Our analysis of almost 150 federal government webpages on LGBTQ-related topics, all of which were created before President Trump took office and continue to be live on the web, reveals that, under the Trump administration, federal government webpages addressing LGBTQ-related topics use the terms “gender” and “transgender” more and the terms “sex” less. However, there is considerable variation between departments and within departments.

Our analysis of 1,875 HHS.gov webpages on all topics for LGBTQ-related terms, showed that LGBTQ-related terms are used less often under the Trump administration with a 25% reduction in the use of the term “gender” and a 40% reduction in the use of “transgender.”
By contrast, the use of terms like “faith-based and community organizations,” “religious freedom,” and “conscience protection” all increased markedly.

Our examination of key case studies of changed LGBTQ-related content on federal agency websites identifies two key trends: 
1. The removal of access to resources about discrimination protections and prevention, especially for transgender individuals
2. The removal of resources containing LGBTQ community-specific information


**Figure. Absolute Changes by Department, August 2019**
![Image](https://github.com/sunlightpolicy/lgbtq_trends/blob/master/src/images/fig3.png "Changes by department")

Read more about our results [here](https://sunlightfoundation.com/web-integrity-project/)

## Repository structure
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
