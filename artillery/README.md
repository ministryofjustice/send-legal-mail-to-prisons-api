# Artillery load/stress tests
[Artillery](https://artillery.io) load/stress tests for the Send Legal Mail To Prisons API

These tests apply load to the SLM API as follows:

* Legal Sender - look up a previously saved contact
* Legal Sender - create a barcode, using the retrieved contact as the recipient
* Mail Staff - check the barcode (aka scan the barcode)
* Mail Staff - indicate the further checks are required for the barcode

The tests present authentication tokens using the Smoke Test users, so the SLM API under test must be configured with
smoke test users.

## Running
To run artillery from your local development machine set the following environment variables in the `.env` file in this
folder:

* `SEND_LEGAL_MAIL_TO_PRISONS_API` - the url of the SLM API that you are testing
* `HMPPS_AUTH_URL` - the url of hmpps-auth, including the `/auth` uri path
* `APP_SMOKETEST_LSJSECRET` - the Legal Sender smoke test user secret as configured in the SLM API under test
* `APP_SMOKETEST_MSJSECRET` - the Mail Staff smoke test user secret as configured in the SLM API under test
* `HMPPS_SYSTEM_CLIENT_ID` - the hmpps-auth system client ID
* `HMPPS_SYSTEM_CLIENT_SECRET` - the hmpps-auth system client secret

eg:
```
SEND_LEGAL_MAIL_TO_PRISONS_API=http://host.docker.internal:8080
HMPPS_AUTH_URL=http://host.docker.internal:9090/auth
APP_SMOKETEST_LSJSECRET=LSJ_SECRET
APP_SMOKETEST_MSJSECRET=MSJ_SECRET
HMPPS_SYSTEM_CLIENT_ID=send-legal-mail-to-prisons-client
HMPPS_SYSTEM_CLIENT_SECRET=clientsecret
```

Once your `.env` file is setup run the `artillery` script in this folder:
```
$ ./artillery
```

### Hostname considerations with localhost
Running artillery via this script uses the artillery docker image - the artillery process and tests are run within a 
container. Therefore specifying the SLM API URL or hmpps-auth URL as `localhost` (assuming they are running on your
machine) will not work as `localhost` will be referencing the container itself (which is not running SLM API or hmpps-auth).  
To resolve this the hostname should be `host.docker.internal` as in the example above. The script sets the docker
configuration to route `host.docker.internal` to the host IP.



