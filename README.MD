## ABOUT

Open-RMBT is an open source, multi-threaded bandwidth test used in [RTR-Netztest](https://www.netztest.at/de/) (RTR-Nettest in English).</p>
This repository contains the source code of the new version of the RTR-Netztest application for Android.</br> The source code of the new implementation is developed by [Specure GmbH](https://martes-specure.com/), based on the [previous app](https://github.com/rtr-nettest/open-rmbt) developed by the Austrian Regulatory Authority for Broadcasting and Telecommunications ([RTR GmbH](https://www.rtr.at/)) and alladin-IT GmbH.</p>
The source code is published under [Apache version 2.0 license](https://github.com/rtr-nettest/open-rmbt-android/blob/master/LICENSE).

### HOW TO BUILD

1. Change app id to preferred one in dependencies.gradle (optional)
2. Update signing config (optional)
	1. Replace signing/debug.keystore with your own one
	2. Update signing/debug.properties with credentials to your signing/debug.keystore key
	3. Replace signing/release.keystore with your own one
	4. Update signing/release.properties with credentials to your signing/release.keystore key
	5. Enable Google Maps SDK in Google Cloud console and put your API key into `googleMapsApiKey` in flavor-config.gradle file
3. Add/change endpoints information and `example.com` in file `app/src/rtr/config.json` 

### VARIABLES DESCRIPTION
##### CONTROL SERVER VARIABLES
- __`CONTROL_SERVER_HOST`__:exclamation:Controßl server host, example: `"sdev.example.com"`
- __`CONTROL_SERVER_CHECK_PRIVATE_IPV4_HOST`__:exclamation:Url to the host for IPv4 test, example `"sdevv4.example.com"`
- __`CONTROL_SERVER_CHECK_PRIVATE_IPV6_HOST`__:exclamation:Url to the host for IPv6 test, example `"sdevv6.example.com"`
- __`CONTROL_SERVER_CHECK_PUBLIC_IPV4_URL`__:exclamation:Link to check public IPv4 address, example `"sdevv4.example.com/RMBTControlServer/ip"`
- __`CONTROL_SERVER_CHECK_PUBLIC_IPV6_URL`__:exclamation:Link to check public IPv6 address, example `"sdevv6.example.com/RMBTControlServer/ip"`
- __`CAPTIVE_PORTAL_WALLED_GARDEN_URL`__:exclamation:Url for checking captive portal presence, example: `"http://webtest.example.com/generate_204"`
- __`CONTROL_SERVER_PORT`__ Control server port, default: `443`
- __`CONTROL_SERVER_USE_SSL`__ Indicates SSL should be used for server connection or not, default: `true`
- __`QOS_SSL`__ Indicates SSL should be used QoS tests, default: `true`

##### TEST SETTINGS
- __`NDT_ENABLED`__ Indicates NDT Tests is enabled, default: `false` _(NDT fuctionality not implemented)_
- __`SKIP_QOS_TESTS`__ Indicates QoS Tests can be skipped after basic measurement, default: `false`
- __`LOOP_MODE_ENABLED`__ Indicates Loop Mode is enabled, default `false`
- __`LOOP_MODE_WAITING_TIME_MIN`__ Default delay between measurements in Loop Mode in minutes, default: `30`
- __`LOOP_MODE_DISTANCE_METERS`__ Default distance between measurements in Loop Mode in meters, default: `250`
- __`LOOP_MODE_NUMBER_OF_TESTS`__ Default number of tests per single Loop Mode test, default: `30`
- __`LOOP_MODE_MAX_NUMBER_OF_TESTS`__ Maximum number of Loop Mode measurements that can be entered by User, default: `100`
- __`LOOP_MODE_MIN_NUMBER_OF_TESTS`__ Minimum number of Loop Mode measurements that can be entered by User, default: `1`
- __`LOOP_MODE_MAX_WAITING_TIME_MIN`__ Maximum delay in minutes between measurements in Loop Mode can be entered by User, default `1440`
- __`LOOP_MODE_MIN_WAITING_TIME_MIN`__ Minimum delay in minutes between measurements in Loop Mode can be entered by User, default `15`
- __`LOOP_MODE_MAX_DISTANCE_METERS`__ Maximum distance in meters between measurements in Loop Mode can be entered by User, default `10000`
- __`LOOP_MODE_MIN_DISTANCE_METERS`__ Minimum distance in meters between measurements in Loop Mode can be entered by User, default `50`
- __`CAPABILITIES_RMBT_HTTP`__ client supports RMBT HTTP, default: `false`
- __`CAPABILITIES_QOS_SUPPORTS_INFO`__ The third state (=INFO) is supported or not, default: `false`
- __`CAPABILITIES_CLASSIFICATION_COUNT`__ Number of intervals(classes) to classify measured values, default: `4`

##### GENERAL SETTINGS
- __`CAN_MANAGE_LOCATION_SETTINGS`__ Settings menu that allows to open application settings (If location permissions are not provided to app) or System Location settings (if location settings disabled for device)

##### EXPERT SETTINGS
- __`EXPERT_MODE_ENABLED`__ Indicates Expert Mode is enabled, default: `false`
- __`EXPERT_MODE_IPV4_ONLY`__ Indicates that only ip4v protocol should be used, default: `false`
- __`USER_SERVER_SELECTION_ENABLED`__ Indicates that user can select server, default: `false`

##### DEVELOPER SETTINGS
- __`DEVELOPER_MODE_IS_ENABLED`__ Indicates Developer Mode is enabled, default: `false`
- __`DEVELOPER_MODE_IS_AVAILABLE`__ Indicates that Developer mode can be activated, default: `true`
- __`IS_MAP_SERVER_OVERRIDE_ENABLED`__ Map server overriding is enabled, default: `false`
- __`IS_CONTROL_SERVER_OVERRIDE_ENABLED`__ Control server overriding is enabled, default: `false`
- __`DEVELOPER_ACTIVATE_CODE`__:exclamation:Code to activate Developer Mode (10 taps on version number in Settings), default: `qwerty`
- __`DEVELOPER_DEACTIVATE_CODE`__:exclamation:Code to deactivate Developer Mode (10 taps on version number), default: `1111`
- __`ALL_DEACTIVATE_CODE`__:exclamation:Code to deactivate everything(?) (10 taps on version number), default: `0000`
- __`DEV_SIMULATE_5G_NETWORK`__:Allows to simulate 5G network in the developer mode
- __`DEV_SIMULATE_5G_NETWORK_IS_AVAILABLE`__:This flag makes simulate 5G network visible in the developer mode or not

##### OTHER VALUES
- __`SOURCE_CODE_URL`__:exclamation:Link to Source Code, value: `https://github.com/rtr-nettest`
- __`WEBSITE_URL`__:exclamation:Link to the website, default: `https://www.example.com`
- __`EMAIL_ADDRESS`__:exclamation:Email address for support, default: `RMBT Support <support@example.com>`
- __`DATA_PRIVACY_AND_TERMS_URL`__:exclamation:Link to Privacy Policy & Terms And Condition, default: `https://example.com/%s/tk/netztestterms`, where `1{%s}` is language code (currently `en` or `de`)
- __`TERMS_FOR_ACCEPTANCE_URL`__:exclamation:Terms and Condition for User acceptance, default: `https://example.com/%s/tc_android.html`, where `1{%s}` is language code (currently `en` or `de`)

##### MAP SETTINGS
- __`MAP_SERVER_HOST`__ Map server host url, default: `sdev.example.com`
- __`MAP_SERVER_PORT`__ Map server port, default: `443`
- __`MAP_SERVER_USE_SSL`__ Indicates SSL should be used for map server connection or not, default: `true`
- __`MAP_SERVER_ROUTE`__ Map server route, default: `RMBTMapServer` 
- __`MAP_TILES_ENDPOINT`__ Map server endpoint to obtain tiles, default: `tiles/%s/%d/%d/%d.png` where `1{%s}` map _type_ (`heatmap` or `points` or `shapes`), `2{%s}` is _zoom_ value, `3{%s}` is _x_ value, `4{%s}` is _y_ value
- __`MAP_MARKERS_ENDPOINT`__ Map server endpoint to obtain markers, default: `tiles/markers`
- __`MAP_MARKER_SHOW_DETAILS_ROUTE`__ Map server route to get marker details, default: `en/Opentest?%s#noMMenu`
- __`MAP_FILTERS_ENDPOINT`__ Map filters endpoint to get map types & filters, default: `tiles/info`

##### CONTROL SERVER ROUTES
- __`CONTROL_SERVER_ROUTE`__ Route to the control server, default:  `RMBTControlServer`
- __`CONTROL_SERVER_SETTINGS_ENDPOINT`__  Endpoint to get settings from the control server, default: `settings`
- __`CONTROL_SERVER_TEST_REQUEST_ENDPOINT`__ Endpoint to get basic measurement set up to start measurement, default: `testRequest` (currently not in use. Control client uses same endpoint from old implementation)
- __`CONTROL_SERVER_SEND_RESULT_ENDPOINT`__ Endpoint to send test results to the control server, default: `result`
- __`CONTROL_SERVER_SEND_QOS_RESULT_ENDPOINT`__ Endpoint to send QoS test results to the control server, default: `resultQoS`
- __`CONTROL_SERVER_HISTORY_ENDPOINT`__ Endpoint to get test history, default: `history`
- __`CONTROL_SERVER_GET_BASIC_RESULT_PATH`__ Endpoint to get test results from the control server, default: `testresult`
- __`CONTROL_SERVER_GET_OPENDATA_RESULT_PATH`__ Endpoint to get open test results from control server, default: `opentests`
- __`CONTROL_SERVER_TEST_RESULT_DETAILS_ENDPOINT`__ Endpoint to get detailed test results, default: `testresultdetail`
- __`CONTROL_SERVER_GET_QOS_TEST_RESULT_ENDPOINT`__ Endpoint to get QoS test results, default: `qosTestResult?api=2`
- __`CONTROL_SERVER_SIGNAL_REQUEST_ROUTE`__ Endpoint to get settings for the continious signal measurement, default: `signalRequest`
- __`CONTROL_SERVER_SIGNAL_RESULT_ROUTE`__ Endpoint to send chunk of results of continiouse signal measurement, default: `signalResult`
- __`CONTROL_SERVER_GET_SYNC_CODE_ROUTE`__ Endpoint to get sync code for current device, default: `sync`
- __`CONTROL_SERVER_SYNC_DEVICES_ROUTE`__ Endpoint to send other device sync code and make devices history synchonised, default `sync` (has different params in api)
- __`CONTROL_SERVER_GET_NEWS_ENDPOINT`__ Endpoint to get news, default: `news`