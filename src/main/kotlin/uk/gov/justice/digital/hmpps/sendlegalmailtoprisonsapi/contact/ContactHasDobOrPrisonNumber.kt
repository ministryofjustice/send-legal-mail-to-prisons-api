package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi.contact

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [ContactHasDobOrPrisonNumberValidator::class])
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class ContactHasDobOrPrisonNumber(
  val message: String = "{javax.validation.constraints.NotBlank.message}",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)

class ContactHasDobOrPrisonNumberValidator : ConstraintValidator<ContactHasDobOrPrisonNumber, CreateContactRequest> {
  override fun isValid(value: CreateContactRequest?, context: ConstraintValidatorContext?): Boolean =
    value != null && (value.dob != null || value.prisonNumber != null)
}
