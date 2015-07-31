//------------------------------------------------------------------------------
// Student Overflow(Bibcast) App with NodeJS on IBM Bluemix
//------------------------------------------------------------------------------

// This application uses HAPI as it's web server
// for more info, see: http://http://hapijs.com/
var hapi = require("hapi");

// cfenv provides access to your Cloud Foundry environment
// for more info, see: https://www.npmjs.com/package/cfenv
var cfenv = require('cfenv');

// Request is designed to be the simplest way possible to make http calls. 
// It supports HTTPS and follows redirects by default. https://github.com/request/request
var request = require("request");


// ------------------------------------------------------------------------------
// Handler for recording server crash or unexpected shutdown
process.on("exit", function(code) {
    console.log("exiting: code: " + code);
});

process.on("uncaughtException", function(err) {
    console.log("uncaught exception: " + err.stack);
    process.exit(1);
});


// ------------------------------------------------------------------------------
// Reading, Registering and initializing Environment variables and credentials

var vcapLocal = null;

try {
    vcapLocal = require("../StudentOverflow_VCAP_Services.json");
} catch (e) {
    console.error("VCAP configuration not found...!!");
}

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

//-----------------------------------------------------------------------------
// Route: Server route to Main page of the application(index.html)
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

//******************************************************************************
// Server is started and running and below are 
//******************************************************************************

var db;

var cloudant;

var dbCredentials = {
    dbName: 'cloud_db'
};

initDBConnection();

/**
 * Initializes connection parameters for connecting to Cloudant Service
 */
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

//-----------------------------------------------------------------------------
// Route: Server route to insert events into Cloudant
server.route({
    method: "POST",
    path: "/res/updateUserLocation/{uid},{locationid},{lon},{lat}",
    handler: insertHandler
});

/**
 * Inserts events from the GEOFENCE notifier into Cloudant
 * @param {Object} request request object from client
 * @param {Object} reply   reply message to client with successful insertion
 */
function insertHandler(request, reply) {

    var uid = request.params.uid,
        locationid = request.params.locationid,
        lon = request.params.lon,
        lat = request.params.lat;

    insertIntoCloudant(uid, locationid, lon, lat, reply);
}

/**
 * Function that performs cloudant DB operations
 * @param   {Number}   uid        Unique User ID
 * @param   {Number}   locationid Fence ID inside a University
 * @param   {Number}   lon        Longitude
 * @param   {Number}   lat        Latitude
 * @param   {Object}   reply      Notifies status of insert operation into cloudant 
 */
function insertIntoCloudant(uid, locationid, lon, lat, reply) {

    console.log('Cloundant insert: ' + uid + ': ' + locationid);

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

//-----------------------------------------------------------------------------
// Route: Server route to get locations and their current capacities
server.route({
    method: "GET",
    path: "/res/requestCurrentInformation",
    handler: getCurrentInformation
});


/**Fetches current filling capacity of the fences defined in the university
 * @param {Object} request request object from client
 * @param {Object} reply   reply message to client with current crowd information
 */
function getCurrentInformation(req, reply) {

    var url = dbCredentials.url;

    request(url + '/cloud_db/_design/map_reduce/_view/count_by_location?group_level=1', function(error, response, body) {
        if (!error && response.statusCode == 200) {
            console.log(body);

            var currentStatus = JSON.parse(body);

            reply(currentStatus);

        } else {
            console.log("Error in retreiving current status: response.statusCode: " + response.statusCode);
        }
    });
}

//-----------------------------------------------------------------------------
// Route: Server route to get locations and their overall capacities
server.route({
    method: "GET",
    path: "/res/requestOverallInformation",
    handler: getOverallInformation
});

/**
 * Fetches information about overall capacity and fence locations
 * @param {Object} request request object from client
 * @param {Object} reply   reply message to client with capacity information
 */
function getOverallInformation(request, reply) {

    db.get("000", {
        revs_info: true
    }, function(err, doc) {
        if (!err) {
            console.log('Fetched details: ' + doc.loc_1.name);
            reply(doc);
        } else {
            console.log('Error in fetching the location base: ' + err);
        }
    });
}