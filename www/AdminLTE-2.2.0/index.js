

var username;

$(onStart);

//------------------------------------------------------------------------------
function onStart() {
    
    var userInput = $("#user-name").val();
    
    username = userInput ? userInput : "Mr.NoName"
    
    
}

function fetchFromCloudant() {
    
    document.getElementById("res").innerHTML = "getCurrentInformation: called";
    
    $.ajax({
        url: "/res/requestCurrentInformation",
        dataType: "json",
        success: function(data, status, jqXhr) {
                document.getElementById("res").innerHTML = "success:" + data[1].key;

        },
        error: function() {
                document.getElementById("res").innerHTML = "failed: ";

        }
    });
    
}


function fetchFromCloudantO() {
    
    document.getElementById("res").innerHTML = "getCurrentInformation: called";
    
    $.ajax({
        url: "/res/requestOverallInformation",
        dataType: "json",
        success: function(data, status, jqXhr) {
                document.getElementById("res").innerHTML = "success:<br>" + data.loc_1.name;

        },
        error: function() {
                document.getElementById("res").innerHTML = "failed: ";

        }
    });
    
}