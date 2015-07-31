function Tester() {
	/* Define test edges */
	var min_longitude, max_longitude, min_latitude, max_latitude;

	min_latitude = 50.120289;
	max_latitude = 50.120295;
	min_longitude = 8.652433;
	max_longitude = 8.652439;

	/* Define location id ranges */
	var loc_min, loc_max;
	loc_min = 0;
	loc_max = 5;

	/* The url which listens to requests */
	var url = "/res/updateUserLocation";

	/* Define a container for generated uid's */
	var uids = [];

	this.fireRandomUserEvent = function() {
		console.log("Firing random user event");
		var uid = "12345";

		/* Push uid to container */
		uids.push(uid);
		fireEvent(uid);
	}

	function fireEvent(uid) {
		var longitude = getRandomArbitrary(min_longitude, max_longitude);
		var latitude = getRandomArbitrary(min_latitude, max_latitude);
		var locationid = getRandomInt(loc_min, loc_max);

		var url_to_call = url + "/" + uid + "," + locationid + "," + longitude
				+ "," + latitude;
		console.log("Calling: " + url_to_call);
		$.ajax({
			method : "POST",
			url : url_to_call,
			context : document.body,
			success : function() {
				$(this).addClass("done");
				console.log("Ajax call was successful.");
			}
		});
	}

	function fireUserEventFromContainer() {
		if (uids.length === 0) {
			console.log("Container is empty");
		}
		var uid = uids.pop();
		fireEvent(uid);
	}

	/**
	 * Returns a random number between min (inclusive) and max (exclusive)
	 */
	function getRandomArbitrary(min, max) {
		return Math.random() * (max - min) + min;
	}

	/**
	 * Returns a random integer between min (inclusive) and max (inclusive)
	 * Using Math.round() will give you a non-uniform distribution!
	 */
	function getRandomInt(min, max) {
		return Math.floor(Math.random() * (max - min + 1)) + min;
	}
}