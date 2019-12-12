'use strict';
const http = require('http');
var assert = require('assert');
const express= require('express');
const app = express();
const mustache = require('mustache');
const filesystem = require('fs');
const url = require('url');
const hbase = require('hbase-rpc-client');
const hostname = '34.66.189.234';
const port = 3889;
const BigIntBuffer = require('bigint-buffer');

var client = hbase({
    zookeeperHosts: ["mpcs53014c10-m-6-20191016152730.us-central1-a.c.mpcs53014-2019.internal:2181"],
    zookeeperRoot: "/hbase-unsecure"
});

client.on('error', function(err) {
  console.log(err)
})


app.use(express.static('public'));
app.get('/delays.html',function (req, res) {
    const topic=req.query['topic'];
    function update_header(top) {
      if (top === "climate") {
        return "Average Temperature (°F), USA";
      } else if (top === "immigration") {
        return "Average New Permanent Residents, USA"
      }
      return "Variable";
    }
    const var_header = update_header(topic)
    console.log(var_header);
    console.log(topic);
    const scan = client.getScanner("andresnz_nyt_hbase");
    scan.setFilter({
	prefixFilter: {
	    prefix: topic
	}
    });
    scan.toArray(function(err, rows) {
	assert.ok(!err, "get returned an error: #{err}");
	if(!rows){
            res.send("<html><body>No such topic in data</body></html>");
            return;
        }
	console.log(rows);
  function divide_by(top, decade) {
    if (top === "climate" && decade === "2011 - 2020") {
      return 97;
    } else if (top === "immigration") {
      return 10;
    }
    return 120;
  }
	var renderedRows = rows.map(function(row) {
	    return {
		decade : row.cols["stats:decade"].value.toString(),
		total_word : Number(BigIntBuffer.toBigIntBE(row.cols["stats:wordcount"].value)),
		total_word_count : Number(BigIntBuffer.toBigIntBE(row.cols["stats:ttalwords"].value)),
    frequency : (Number(BigIntBuffer.toBigIntBE(row.cols["stats:wordcount"].value)) * 100 / Number(BigIntBuffer.toBigIntBE(row.cols["stats:ttalwords"].value))).toFixed(3),
    average : (Number(BigIntBuffer.toBigIntBE(row.cols["stats:value"].value)) / divide_by(topic, row.cols["stats:decade"].value.toString())).toFixed(0)
	    }
	});
	console.log(renderedRows);
	// console.log(rows[0].cols["stats:year"].value.toString());
	// console.log(rows[0].cols["stats:clear_flights"].value);
	// console.log(parseInt(rows[0].cols["stats:clear_flights"].value.toString()));
	// console.log(rows[0].cols["stats:clear_ontime"].value.readIntLE());

	var template = filesystem.readFileSync("result.mustache").toString();
	var html = mustache.render(template,  {
	    topic: topic,
      value: var_header,
	    row: renderedRows
	});
	res.send(html);

    });
});

/* Send simulated titles to kafka */
var kafka = require('kafka-node');
var Producer = kafka.Producer;
var KeyedMessage = kafka.KeyedMessage;
var kafkaClient = new kafka.KafkaClient({kafkaHost: 'mpcs53014c10-m-6-20191016152730.us-central1-a.c.mpcs53014-2019.internal:6667'});
var kafkaProducer = new Producer(kafkaClient);

app.get('/weather.html',function (req, res) {
    var climate_c = req.query['climate_c'];
    var immigration_c = req.query['immigration_c'];
    var other_c = req.query['other_c'];
    var report = {
      climate: climate_c,
      immigration: immigration_c,
      other: other_c
    };
    kafkaProducer.send([{ topic: 'andresnz_kafka_wc', messages: JSON.stringify(report)}],
			   function (err, data) {
			       console.log(data);
			   });
    console.log(report);
    res.redirect('submit-counts.html');
});

app.listen(port);
