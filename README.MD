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
- `CONTROL_SERVER_HOST`:exclamation:Control server host, example: `"sdev.example.com"`
- `CONTROL_SERVER_CHECK_PRIVATE_IPV4_HOST`:exclamation:Url to the host for IPv4 test, example `"sdevv4.example.com"`
- `CONTROL_SERVER_CHECK_PRIVATE_IPV6_HOST`:exclamation:Url to the host for IPv6 test, example `"sdevv6.example.com"`
- `CONTROL_SERVER_CHECK_PUBLIC_IPV4_URL`:exclamation:Link to check public IPv4 address, example `"sdevv4.example.com/RMBTControlServer/ip"`
- `CONTROL_SERVER_CHECK_PUBLIC_IPV6_URL`:exclamation:Link to check public IPv6 address, example `"sdevv6.example.com/RMBTControlServer/ip"`
- `CAPTIVE_PORTAL_WALLED_GARDEN_URL`:exclamation:Url for checking captive portal presence, example: `"http://webtest.example.com/generate_204"`
- `CONTROL_SERVER_PORT` Control server port, default: `443`
- `CONTROL_SERVER_USE_SSL` Indicates SSL should be used for server connection or not, default: `true`
- `QOS_SSL` Indicates SSL should be used QoS tests, default: `true`

##### TEST SETTINGS
- `NDT_ENABLED` Indicates NDT Tests is enabled, default: `false` _(NDT fuctionality not implemented)_
- `SKIP_QOS_TESTS` Indicates QoS Tests can be skipped after basic measurement, default: `false`
- `LOOP_MODE_ENABLED` Indicates Loop Mode is enabled, default `false`
- `LOOP_MODE_WAITING_TIME_MIN` Default delay between measurements in Loop Mode in minutes, default: `30`
- `LOOP_MODE_DISTANCE_METERS` Default distance between measurements in Loop Mode in meters, default: `250`
- `LOOP_MODE_NUMBER_OF_TESTS` Default number of tests per single Loop Mode test, default: `30`
- `LOOP_MODE_MAX_NUMBER_OF_TESTS` Maximum number of Loop Mode measurements that can be entered by User, default: `100`
- `LOOP_MODE_MIN_NUMBER_OF_TESTS` Minimum number of Loop Mode measurements that can be entered by User, default: `1`
- `LOOP_MODE_MAX_WAITING_TIME_MIN` Maximum delay in minutes between measurements in Loop Mode can be entered by User, default `1440`
- `LOOP_MODE_MIN_WAITING_TIME_MIN` Minimum delay in minutes between measurements in Loop Mode can be entered by User, default `15`
- `LOOP_MODE_MAX_DISTANCE_METERS` Maximum distance in meters between measurements in Loop Mode can be entered by User, default `10000`
- `LOOP_MODE_MIN_DISTANCE_METERS` Minimum distance in meters between measurements in Loop Mode can be entered by User, default `50`
- `CAPABILITIES_RMBT_HTTP` client supports RMBT HTTP, default: `false`
- `CAPABILITIES_QOS_SUPPORTS_INFO` The third state (=INFO) is supported or not, default: `false`
- `CAPABILITIES_CLASSIFICATION_COUNT` Number of intervals(classes) to classify measured values, default: `4`

##### GENERAL SETTINGS
- `CAN_MANAGE_LOCATION_SETTINGS` Settings menu that allows to open application settings (If location permissions are not provided to app) or System Location settings (if location settings disabled for device)

##### EXPERT SETTINGS
- `EXPERT_MODE_ENABLED` Indicates Expert Mode is enabled, default: `false`
- `EXPERT_MODE_IPV4_ONLY` Indicates that only ip4v protocol should be used, default: `false`
- `USER_SERVER_SELECTION_ENABLED` Indicates that user can select server, default: `false`

##### DEVELOPER SETTINGS
- `DEVELOPER_MODE_IS_ENABLED` Indicates Developer Mode is enabled, default: `false`
- `DEVELOPER_MODE_IS_AVAILABLE` Indicates that Developer mode can be activated, default: `true`
- `IS_MAP_SERVER_OVERRIDE_ENABLED` Map server overriding is enabled, default: `false`
- `IS_CONTROL_SERVER_OVERRIDE_ENABLED` Control server overriding is enabled, default: `false`
- `DEVELOPER_ACTIVATE_CODE`:exclamation:Code to activate Developer Mode (10 taps on version number in Settings), default: `qwerty`
- `DEVELOPER_DEACTIVATE_CODE`:exclamation:Code to deactivate Developer Mode (10 taps on version number), default: `1111`
- `ALL_DEACTIVATE_CODE`:exclamation:Code to deactivate everything(?) (10 taps on version number), default: `0000`

##### OTHER VALUES
- `SOURCE_CODE_URL`:exclamation:Link to Source Code, value: `https://github.com/rtr-nettest`
- `WEBSITE_URL`:exclamation:Link to the website, default: `https://www.example.com`
- `EMAIL_ADDRESS`:exclamation:Email address for support, default: `RMBT Support <support@example.com>`
- `DATA_PRIVACY_AND_TERMS_URL`:exclamation:Link to Privacy Policy & Terms And Condition, default: `https://example.com/%s/tk/netztestterms`, where `1{%s}` is language code (currently `en` or `de`)
- `TERMS_FOR_ACCEPTANCE_URL`:exclamation:Terms and Condition for User acceptance, default: `https://example.com/%s/tc_android.html`, where `1{%s}` is language code (currently `en` or `de`)

##### MAP SETTINGS
- `MAP_SERVER_HOST` Map server host url, default: `sdev.example.com`
- `MAP_SERVER_PORT` Map server port, default: `443`
- `MAP_SERVER_USE_SSL` Indicates SSL should be used for map server connection or not, default: `true`
- `MAP_SERVER_ROUTE` Map server route, default: `RMBTMapServer` 
- `MAP_TILES_ENDPOINT` Map server endpoint to obtain tiles, default: `tiles/%s/%d/%d/%d.png` where `1{%s}` map _type_ (`heatmap` or `points` or `shapes`), `2{%s}` is _zoom_ value, `3{%s}` is _x_ value, `4{%s}` is _y_ value
- `MAP_MARKERS_ENDPOINT` Map server endpoint to obtain markers, default: `tiles/markers`
- `MAP_MARKER_SHOW_DETAILS_ROUTE` Map server route to get marker details, default: `en/Opentest?%s#noMMenu`
- `MAP_FILTERS_ENDPOINT` Map filters endpoint to get map types & filters, default: `tiles/info`

##### CONTROL SERVER ROUTES
- `CONTROL_SERVER_ROUTE` Route to the control server, default:  `RMBTControlServer`
- `CONTROL_SERVER_SETTINGS_ENDPOINT`  Endpoint to get settings from the control server, default: `settings`
- `CONTROL_SERVER_TEST_REQUEST_ENDPOINT` Endpoint to get basic measurement set up to start measurement, default: `testRequest` (currently not in use. Control client uses same endpoint from old implementation)
- `CONTROL_SERVER_SEND_RESULT_ENDPOINT` Endpoint to send test results to the control server, default: `result`
- `CONTROL_SERVER_SEND_QOS_RESULT_ENDPOINT` Endpoint to send QoS test results to the control server, default: `resultQoS`
- `CONTROL_SERVER_HISTORY_ENDPOINT` Endpoint to get test history, default: `history`
- `CONTROL_SERVER_GET_BASIC_RESULT_PATH` Endpoint to get test results from the control server, default: `testresult`
- `CONTROL_SERVER_GET_OPENDATA_RESULT_PATH` Endpoint to get open test results from control server, default: `opentests`
- `CONTROL_SERVER_TEST_RESULT_DETAILS_ENDPOINT` Endpoint to get detailed test results, default: `testresultdetail`
- `CONTROL_SERVER_GET_QOS_TEST_RESULT_ENDPOINT` Endpoint to get QoS test results, default: `qosTestResult?api=2`
- `CONTROL_SERVER_SIGNAL_REQUEST_ROUTE` Endpoint to get settings for the continious signal measurement, default: `/signalRequest`
- `CONTROL_SERVER_SIGNAL_RESULT_ROUTE` Endpoint to send chunk of results of continiouse signal measurement, default: `/signalResult`
- `CONTROL_SERVER_GET_SYNC_CODE_ROUTE` Endpoint to get sync code for current device, default: `sync`
- `CONTROL_SERVER_SYNC_DEVICES_ROUTE` Endpoint to send other device sync code and make devices history synchonised, default `sync` (has different params in api)