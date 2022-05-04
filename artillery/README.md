# Artillery load/stress tests
[Artillery](https://artillery.io) load/stress tests for Send Legal Mail To Prisons API

These tests apply load to the SLM API as follows:

* Legal Sender - look up a previously saved contact
* Legal Sender - create a barcode, using the retrieved contact as the recipient
* Mail Staff - check the barcode (aka scan the barcode)
* Mail Staff - indicate the further checks are required for the barcode

The above load is applied in 3 phases - warm up, ramp up, and sustained load.

* The warm up phase runs for 10 seconds using 1 virtual user (where each virtual user runs the above 4 steps).
* The ramp up phase runs for 30 seonds, ramping up the 1 virtual user to 20 virtual users.
* The sustained load phase runs for 300 seconds, sustaining 20 virtual users.

These parameters can be tweaked (see below).

The tests present authentication tokens using the Smoke Test users.

## Running locally
To run artillery from your local development machine run the `artillery` script in this folder:
```
$ ./artillery
```
The Send Legal Mail To Prisons API is run up in a docker environment complete with all supporting containers. Running
the artillery tests locally runs the tests against the API running in the docker environment. You do not need to have
any services running locally on your machine; they are all spun up via docker-compose, and destroyed at the end.

### Tweaking test parameters
The test parameters can be tweaked using arguments to the `artillery` command:
```
------------------------------------------------------------------------------
  Step/Metric                           Value   CLI Option
------------------------------------------------------------------------------
  Warm up duration (seconds)            10      --warm-up-duration
  Warm up with users                    1       --warm-up-vusers
  Ramp up duration (seconds)            30      --ramp-up-duration
  Exit ramp up with users               20      --ramp-up-target-vusers
  Sustained load duration (seconds)     300     --sustained-load-duration
  Sustained load users                  20      --sustained-load-vusers
------------------------------------------------------------------------------
```
for example:
```
$ ./artillery --ramp-up-target-vusers 15 --sustained-load-vusers 15 --sustained-load-duration 600
```

## Reports
The output of these tests is a html file which charts various metrics of the test run and a json file containing the raw
data:
* 
* `test-run-report.json.hml`
* `test-run-report.json`

* Both of these files are included in the `.gitignore` file and should not be committed to source control.

## Benchmark Performance Tests
An overnight CircleCI job runs some benchmark tests against the API and records the results in Application Insights Logs as CustomEvents with name `performance-benchmarks`.

The trigger for the job can be found in the CircleCI configuration file `/circleci/config.yml` and is called `artillery-nightly`.

### Benchmark Test Results
The recent test results can be found on the [SLM Performance Azure dashboard](https://portal.azure.com/#@nomsdigitechoutlook.onmicrosoft.com/dashboard/arm/subscriptions/c27cfedb-f5e9-45e6-9642-0fad1a5c94e7/resourcegroups/nomisapi-t3-alerts-rg/providers/microsoft.portal/dashboards/d0c0c0d9-0784-45eb-815b-73ce8c42d5c5).

This shows the number of API calls made in the time available and the number of API calls that failed:
* a drop in the number of API calls made indicates that the performance of the API is degraded
* an increase in the number of failed API calls indicates that the performance of the API is degraded

### Benchmark Test Alerts
We have 2 alerts to notify us when the API performance is degraded:
* [alert if number of API calls completed reduces significantly](https://portal.azure.com/#blade/Microsoft_Azure_Monitoring/UpdateLogSearchV2AlertRuleViewModel/alertId/%2Fsubscriptions%2Fc27cfedb-f5e9-45e6-9642-0fad1a5c94e7%2FresourceGroups%2Fnomisapi-t3-rg%2Fproviders%2Fmicrosoft.insights%2Fscheduledqueryrules%2FSLM%20-%20Performance%20Benchmark%20dropped%20by%2020%20percent)
* [alert if number of failed API calls increases](https://portal.azure.com/#blade/Microsoft_Azure_Monitoring/UpdateLogSearchV2AlertRuleViewModel/alertId/%2Fsubscriptions%2Fc27cfedb-f5e9-45e6-9642-0fad1a5c94e7%2FresourceGroups%2Fnomisapi-t3-rg%2Fproviders%2Fmicrosoft.insights%2Fscheduledqueryrules%2FSLM%20Performance%20errors%20more%20than%20recent%20average)

These alerts should trigger notifications in the `#farsight-alerts` channel on MOJ Slack.

### What should I do if performance is degraded?
This hasn't happened yet so this section is a bit theoretical. Please update this section if you get some real life experience!

Some ideas for next steps are:
* look further back through the [Benchmark Test Results](#benchmark-test-results) to try to identify when the performance first started degrading
* investigate commits that occurred around the same time as the performance degradation
* be suspicious of any changes to the database or the API
* also be on lookout for changes to infrastructure, base images, JVM version, database version, library versions etc.

