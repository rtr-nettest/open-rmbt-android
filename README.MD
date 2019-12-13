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
3. Add/change endpoints information in file `app/src/rtr/config.json`
    1. **CONTROL_SERVER_HOST** - Control server host, example "myhost.com"
    2. **CONTROL_SERVER_CHECK_PRIVATE_IPV4_HOST** - Url to the host for IPv4 test, example "v4.myhost.com"
    3. **CONTROL_SERVER_CHECK_PRIVATE_IPV6_HOST** - Url to the host for IPv6 test, example "v6.myhost.com"
    4. **CONTROL_SERVER_CHECK_PUBLIC_IPV4_URL** - Link to check public IPv4 address, example "v4.myhost.com/ControlServer/V2/ip
    5. **CONTROL_SERVER_CHECK_PUBLIC_IPV6_URL** - Link to check public IPv6 address, example "v6.myhost.com/ControlServer/V2/ip
    6. **CONTROL_SERVER_PORT** - control server port 443 by default
    7. **CONTROL_SERVER_USE_SSL** - indicates SSL should be used for server connection or not
    8. **CONTROL_SERVER_ROUTE** - rote to the control server, example for value "RMBTControlServer", "myhost.com/RMBTControlServer/endpoint" will be used for requests to ControlServer
    8. **CONTROL_SERVER_SETTINGS_ENDPOINT** - endpoint name for getting settings from the control server, example "settings"
    9. **CONTROL_SERVER_TEST_REQUEST_PATH** - endpoint name for getting basic measurement set up to start measurement, example "testRequest"
    10. **CONTROL_SERVER_SEND_RESULT_PATH** - endpoint name for sending test results to the control server, example "result"
    12. **CAPABILITIES_RMBT_HTTP** - client supports RMBT HTTP, example "false"
    12. **CAPABILITIES_QOS_SUPPORTS_INFO** - if "true" the third state (=INFO) is supported, example "false"
    13. **CAPABILITIES_CLASSIFICATION_COUNT** - number of intervals(classes) to classify measured values, example "4"