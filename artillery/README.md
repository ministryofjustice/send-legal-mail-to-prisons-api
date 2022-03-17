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



