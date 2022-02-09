package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.barcode

import org.assertj.core.api.AbstractAssert

/**
 * AssertJ custom Assert class for [BarcodeRecipient]
 */
class BarcodeRecipientAssert(actual: BarcodeRecipient?) :
  AbstractAssert<BarcodeRecipientAssert, BarcodeRecipient>(actual, BarcodeRecipientAssert::class.java) {

  companion object {
    fun assertThat(actual: BarcodeRecipient?) = BarcodeRecipientAssert(actual)
  }

  /**
   * Asserts the equality of the actual and expected [BarcodeRecipient]s by comparing all fields except the ID field.
   */
  fun hasFieldsEqualToFieldsOf(expected: BarcodeRecipient): BarcodeRecipientAssert {
    isNotNull
    if (actual.barcode != expected.barcode) {
      failWithMessage("Expected Barcode to be ${expected.barcode.code} but was ${actual.barcode.code}")
    }
    if (actual.name != expected.name) {
      failWithMessage("Expected recipient name to be ${expected.name} but was ${actual.name}")
    }
    if (actual.prisonCode != expected.prisonCode) {
      failWithMessage("Expected recipient prison code to be ${expected.prisonCode} but was ${actual.prisonCode}")
    }
    if (actual.dob != expected.dob) {
      failWithMessage("Expected recipient DOB to be ${expected.dob} but was ${actual.dob}")
    }
    if (actual.prisonNumber != expected.prisonNumber) {
      failWithMessage("Expected recipient prison number to be ${expected.prisonNumber} but was ${actual.prisonNumber}")
    }
    return this
  }
}
