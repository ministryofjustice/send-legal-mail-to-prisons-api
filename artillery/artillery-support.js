/**
 * Javascript support module for artillery, allowing the use of beforeScenario, afterScenario, beforeRequest and afterResponse
 * hooks. See https://www.artillery.io/docs/guides/guides/http-reference#function-signatures
 *
 */
module.exports = {
    exampleBeforeRequestHandler(req, ctx, ee, next) {
        return next() // the handler must return next() in order for the tests to continue
    },

    exampleAfterResponseHandler(req, res, ctx, ee, next) {
        return next() // the handler must return next() in order for the tests to continue
    },

    /*
     * beforeRequest handler to set the Basic Auth digest from the HMPPS client ID and client secrets.
     * The digest is added as a context variable `hmpps_basic_auth_digest` that can then be referenced in the request header.
     */
    setHmppsBasicAuthDigest(req, ctx, ee, next) {
        const digest = Buffer.from(`${ctx.vars['hmpps_system_client_id']}:${ctx.vars['hmpps_system_client_secret']}`).toString('base64')
        ctx.vars['hmpps_basic_auth_digest'] = digest
        return next()
    },

    /*
     * Handler that can be used as an `afterResponse` handler to aid debugging:
     *   afterResponse: 'logResponse'
     */
    logResponse(req, res, ctx, ee, next) {
        console.log('Request', req)
        console.log('Response status', res.statusCode)
        console.log('Response body', res.body)
        return next()
    },
}
