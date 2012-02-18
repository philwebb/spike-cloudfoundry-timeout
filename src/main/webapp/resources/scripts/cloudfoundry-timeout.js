(function() {
	var TIMEOUT = 1000 * 60;

	// Replace XHR methods with long poll aware versions
	dojo.xhrGet = xhrLongPollOnTimeout(dojo.xhrGet);
	dojo.xhrPost = xhrLongPollOnTimeout(dojo.xhrPost);
	dojo.xhrPut = xhrLongPollOnTimeout(dojo.xhrPut);
	dojo.xhrDelete = xhrLongPollOnTimeout(dojo.xhrDelete);
	dojo.rawXhrPost = xhrLongPollOnTimeout(dojo.rawXhrPost);

	/**
	 * Create a new xhr function that switches to long-polling for a response on
	 * a gateway timeout.
	 * 
	 * @param originalXhr
	 *            the original xhr function
	 * @return a xhr function that supports long poll on timeout
	 */
	function xhrLongPollOnTimeout(originalXhr) {
		return function(args) {
			dojo.require("dojox.uuid.generateRandomUuid");

			var requestId = dojox.uuid.generateRandomUuid();
			var newArgs = dojo.mixin({}, args);
			var requestHeader = {
				"x-cloudfoundry-timeout-protection-initial-request" : requestId
			};
			var timeout = null;

			// Add a header, remove any load and error functions and attach our
			// own handle
			newArgs.headers = dojo.mixin(dojo.clone(args.headers),
					requestHeader);
			delete newArgs.load;
			delete newArgs.error;
			newArgs.handle = handleXhr;
			newArgs.failOk = true;

			// Call the original DOJO implementation with our new args
			originalXhr(newArgs);

			function handleXhr(result, ioargs) {
				if (ioargs.xhr.status === 504) {
					// Handle gateway timeout by switching to long polling
					timeout = setTimeout(function() {
						callOriginalHandlers(result, ioargs);
						timeout = null;
					}, TIMEOUT);
					longPollForResult();
				} else {
					callOriginalHandlers(result, ioargs);
				}
			}

			function longPollForResult() {
				originalXhr({
					headers : {
						"x-cloudfoundry-timeout-protection-poll" : requestId
					},
					url : args.url,
					handle : function(result, ioargs) {
						if (ioargs.xhr.status === 204) {
							// No content returned as yet, continue to poll
							if (timeout) {
								longPollForResult();
							}
						} else {
							clearTimeout(timeout);
							callOriginalHandlers(result, ioargs);
						}
					}
				});
			}

			function callOriginalHandlers(result, ioargs) {
				var isError = (result instanceof Error);
				if (dojo.isFunction(args.handle)) {
					args.handle(result, ioargs);
				}
				if (dojo.isFunction(args.error) && isError) {
					args.error(result, ioargs);
				}
				if (dojo.isFunction(args.load) && !isError) {
					args.load(result, ioargs);
				}
			}
		};
	}
})();
