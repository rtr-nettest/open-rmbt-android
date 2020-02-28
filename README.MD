## ABOUT

Open-RMBT is an open source, multi-threaded bandwidth test used in [RTR-Netztest](https://www.netztest.at/de/) (RTR-Nettest in English).</p>
This repository contains the source code of the new version of the RTR-Netztest application for Android.</br> The source code of the new implementation is developed by [Specure GmbH](https://martes-specure.com/), based on the [previous app](https://github.com/rtr-nettest/open-rmbt) developed by the Austrian Regulatory Authority for Broadcasting and Telecommunications ([RTR GmbH](https://www.rtr.at/)) and alladin-IT GmbH.</p>
The source code is published under [Apache version 2.0 license](https://github.com/rtr-nettest/open-rmbt-android/blob/master/LICENSE).

## HOW TO BUILD

1. Change app id to preferred one in dependencies.gradle (optional)
2. Update signing config (optional)
	1. Replace signing/debug.keystore with your own one
	2. Update signing/debug.properties with credentials to your signing/debug.keystore key
	3. Replace signing/release.keystore with your own one
	4. Update signing/release.properties with credentials to your signing/release.keystore key
	5. Enable Google Maps SDK in Google Cloud console and put your API key into `googleMapsApiKey` in flavor-config.gradle file
3. Add/change endpoints information in file `app/src/rtr/config.json`
    1. **CONTROL_SERVER_HOST** - Control server host, example "example.com"
    2. **CONTROL_SERVER_CHECK_PRIVATE_IPV4_HOST** - Url to the host for IPv4 test, example "v4.example.com"
    3. **CONTROL_SERVER_CHECK_PRIVATE_IPV6_HOST** - Url to the host for IPv6 test, example "v6.example.com"
    4. **CONTROL_SERVER_CHECK_PUBLIC_IPV4_URL** - Link to check public IPv4 address, example "v4.example.com/ControlServer/V2/ip
    5. **CONTROL_SERVER_CHECK_PUBLIC_IPV6_URL** - Link to check public IPv6 address, example "v6.example.com/ControlServer/V2/ip
    6. **CONTROL_SERVER_PORT** - control server port 443 by default
    7. **CONTROL_SERVER_USE_SSL** - indicates SSL should be used for server connection or not
    8. **CONTROL_SERVER_ROUTE** - route to the control server, example for value "RMBTControlServer", "example.com/RMBTControlServer/endpoint" will be used for requests to ControlServer
    9. **CONTROL_SERVER_SETTINGS_ENDPOINT** - endpoint name for getting settings from the control server, example "settings"
    10. **CONTROL_SERVER_TEST_REQUEST_PATH** - endpoint name for getting basic measurement set up to start measurement, example "testRequest"
    11. **CONTROL_SERVER_SEND_RESULT_PATH** - endpoint name for sending test results to the control server, example "result"
    12. **CONTROL_SERVER_SEND_QOS_RESULT_ENDPOINT** - endpoint name for sending QoS test results to the control server, example "result"
    13. **CONTROL_SERVER_GET_BASIC_RESULT_PATH** - endpoint name for getting test results from the control server, example "testresult"
    14. **CONTROL_SERVER_GET_OPENDATA_RESULT_PATH** - endpoint name for getting open test results from the control server, example "opentests/[test_open_uuid]"
    15. **CONTROL_SERVER_GET_QOS_TEST_RESULT_ENDPOINT** - endpoint name for getting qos test results from the control server, example "qostestresult"
    16. **CAPTIVE_PORTAL_WALLED_GARDEN_URL** - url for checking captive portal presence
    17. **WEBSITE_URL** - url of web page user will be redirected to after click on the web page item in settings
    18. **EMAIL_ADDRESS** - email address to write on from contact item in settings
    19. **SOURCE_CODE_URL** - url to public github repository with app code
    20. **CAPABILITIES_RMBT_HTTP** - client supports RMBT HTTP, example "false"
    21. **CAPABILITIES_QOS_SUPPORTS_INFO** - if "true" the third state (=INFO) is supported, example "false"
    22. **CAPABILITIES_CLASSIFICATION_COUNT** - number of intervals(classes) to classify measured values, example "4"
    23. **IS_CONTROL_SERVER_OVERRIDE_ENABLED** - Control server override enabled false by default
    24. **DATA_PRIVACY_AND_TERMS_URL** - url of data privacy and terms of use