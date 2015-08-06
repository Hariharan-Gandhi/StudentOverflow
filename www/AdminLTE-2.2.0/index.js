var username;

$(onStart);

//------------------------------------------------------------------------------
function onStart() {

    var userInput = $("#user-name").val();

    username = userInput ? userInput : "Mr.NoName"


}

$(document).on("mouseleave","#architecture",function(e){
   $("[data-widget='collapse']").click();
});

$(document).on("mouseenter","#architecture",function(e){
   $("[data-widget='collapse']").click();
});
