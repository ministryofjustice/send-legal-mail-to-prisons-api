if (!process.env['APPLICATIONINSIGHTS_CONNECTION_STRING'] || !process.env['BUILD_NUMBER']) {
    console.log('Environment variables APPLICATIONINSIGHTS_CONNECTION_STRING and BUILD_NUMBER not set. Cannot continue')
    process.exit(1)
}

// Setup the App Insights process and it's default client
const appInsights = require('applicationinsights');
const { DistributedTracingModes } = require("applicationinsights");
appInsights.setup(process.env['APPLICATIONINSIGHTS_CONNECTION_STRING']).setDistributedTracingMode(DistributedTracingModes.AI_AND_W3C).start();
appInsights.defaultClient.context.tags['ai.cloud.role'] = 'send-legal-mail-to-prisons-api'
appInsights.defaultClient.context.tags['ai.application.ver'] = process.env['BUILD_NUMBER'] || ''

// Load the test run report data (test-run-report.json is the output from artillery)
const testRunData = require('./test-run-report.json')
// We are only interested in it's counters object
const { counters } = testRunData.aggregate

// Send it as a 'performance-benchmarks' event to App Insights
appInsights.defaultClient.trackEvent({
    name: 'performance-benchmarks',
    properties: counters
})

