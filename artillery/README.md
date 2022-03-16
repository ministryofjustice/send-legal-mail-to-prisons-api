# Artillery load/stress tests
[Artillery](https://artillery.io) load/stress tests for Send Legal Mail To Prisons API

These tests apply load to the SLM API as follows:

* Legal Sender - look up a previously saved contact
* Legal Sender - create a barcode, using the retrieved contact as the recipient
* Mail Staff - check the barcode (aka scan the barcode)
* Mail Staff - indicate the further checks are required for the barcode

The tests present authentication tokens using the Smoke Test users

## Running locally
To run artillery from your local development machine run the `artillery` script in this folder:
```
$ ./artillery
```
The Send Legal Mail To Prisons API is run up in a docker environment complete with all supporting containers. Running
the artillery tests locally runs the tests against the API running in the docker environment. You do not need to have
any services running locally on your machine; they are all spun up via docker-compose, and destroyed at the end.

## Reports
The output of these tests is a html file which charts various metrics of the test run and a json file containing the raw
data:
* 
* `test-run-report.json.hml`
* `test-run-report.json`

* Both of these files are included in the `.gitignore` file and should not be committed to source control.



