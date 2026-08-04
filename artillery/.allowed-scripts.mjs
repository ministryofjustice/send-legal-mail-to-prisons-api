import { configureAllowedScripts } from '@ministryofjustice/hmpps-npm-script-allowlist'

export default configureAllowedScripts({
  allowlist: {
    "node_modules/protobufjs@7.6.5": "FORBID"
  },
})
