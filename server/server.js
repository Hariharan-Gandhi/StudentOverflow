//------------------------------------------------------------------------------
// Student Overflow App with NodeJS on IBM Bluemix
//------------------------------------------------------------------------------

var hapi = require("hapi");
var cfenv = require("cfenv");
var Boom = require('boom');
var fs = require('fs');


// This application uses express as it's web server
// for more info, see: http://expressjs.com
var express = require('express');

// cfenv provides access to your Cloud Foundry environment
// for more info, see: https://www.npmjs.com/package/cfenv
var cfenv = require('cfenv');

// create a new express server
/*var app = express();

// serve the files out of ./public as our main files
app.use(express.static(__dirname + '/public'));

// get the app environment from Cloud Foundry
var appEnv = cfenv.getAppEnv();

// start server on the specified port and binding host
app.listen(appEnv.port, appEnv.bind, function() {

	// print a message when the server starts listening
  console.log("server starting on " + appEnv.url);
}); */


var vcapLocal = null
try {
    vcapLocal = require("../StudentOverflow_VCAP_Services.json");
} catch (e) {
    console.error("VCAP configuration not found...!!");
}

// ------------------------------------------------------------------------------
process.on("exit", function(code) {
    console.log("exiting: code: " + code);
});

process.on("uncaughtException", function(err) {
    console.log("uncaught exception: " + err.stack);
    process.exit(1);
});

// ------------------------------------------------------------------------------
var appEnvOpts = vcapLocal ? {
    vcap: vcapLocal
} : {};

var appEnv = cfenv.getAppEnv(appEnvOpts);

console.log("Services: " + JSON.stringify(appEnv.services));
var server = new hapi.Server();

server.connection({
    host: appEnv.bind,
    port: appEnv.port
});


//Main page - Route
server.route({
    method: "GET",
    path: "/{param*}",
    handler: {
        directory: {
            path: "www"
        }
    }
});

console.log("Student Overload Server Starting...");

server.start(function() {
    console.log("Student Overload Server Started!! --- " + appEnv.url);
});

console.log("Couldant Services: " + JSON.stringify(appEnv.services.cloudantNoSQLDB));

//----------------

var db;

var cloudant;

var dbCredentials = {
    dbName: 'cloud_db'
};

initDBConnection();

function initDBConnection() {

    var serviceCreds = appEnv.getServiceCreds("Cloudant_SO");

    if (!serviceCreds) {
        console.log("service 'cloudant' not bound to this application");
        return;
    }

    dbCredentials.host = serviceCreds.host;
    dbCredentials.port = serviceCreds.port;
    dbCredentials.user = serviceCreds.username;
    dbCredentials.password = serviceCreds.password;
    dbCredentials.url = serviceCreds.url;

    console.log('VCAP Services: ' + JSON.stringify(dbCredentials));

    /*dbCredentials.host = "ffe37731-0505-4683-96a8-87d02a33e03e-bluemix.cloudant.com";
    dbCredentials.port = 443;
    dbCredentials.user = "ffe37731-0505-4683-96a8-87d02a33e03e-bluemix";
    dbCredentials.password = "c7003d0b156d9c4ce856c4e6b4427f3b576c7ea6229235f0369ada1ed47b159c";
    dbCredentials.url = "https://ffe37731-0505-4683-96a8-87d02a33e03e-bluemix:c7003d0b156d9c4ce856c4e6b4427f3b576c7ea6229235f0369ada1ed47b159c@ffe37731-0505-4683-96a8-87d02a33e03e-bluemix.cloudant.com";*/

    cloudant = require('cloudant')(dbCredentials.url);

    //check if DB exists if not create
    cloudant.db.create(dbCredentials.dbName, function(err, res) {
        if (err) {
            console.log('could not create db ', err);
        }
        console.log('Created Database successfully:' + res);
    });

    db = cloudant.use(dbCredentials.dbName);


}

function insertIntoCloudantSample(uid, locationid, lon, lat, reply) {

    console.log('Cloundant insert: ' + uid + ': '+locationid);

    db.get(uid, {
        revs_info: true
    }, function(err, doc) {
        if (!err) {
            console.log('Document already exists: ' + doc.locationid);
            doc.locationid = locationid;
            db.insert(doc, doc.id, function(err, doc) {
                if (err) {
                    console.log('Error inserting data\n' + err);
                    return 500;
                }
                return 200;
            });

        } else {
            console.log('Document doesnot exists: ' + err);
            db.insert({
                locationid: locationid,
                lon: lon,
                lat: lat
            }, uid, function(err, doc) {
                if (err) {
                    console.log(err);
                    reply("Error in saving the document: " + err);
                } else {
                    reply('Document saved successfully for : ' + uid);
                    console.log('Document saved successfully for : ' + uid);
                }
            });
        }
    });
}


//https://74520cc5-c1d6-44bf-80f1-e507f648678d-bluemix.cloudant.com/cloud_db/_design/map_reduce/_view/count_by_location


// Route calls to insert in cloundant
server.route({
    method: "POST",
    path: "/res/updateUserLocation/{uid},{locationid},{lon},{lat}",
    handler: insertIntoCloudant
});

function insertIntoCloudant(request, reply) {

    var uid = request.params.uid,
        locationid = request.params.locationid,
        lon = request.params.lon,
        lat = request.params.lat;

    insertIntoCloudantSample(uid, locationid, lon, lat, reply);

}