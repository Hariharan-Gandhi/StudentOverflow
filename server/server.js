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

server.start(function(){
    console.log("Student Overload Server Started!!")
});