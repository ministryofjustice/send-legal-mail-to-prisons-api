package uk.gov.justice.digital.hmpps.sendlegalmailtoprisonsapi

import java.util.Optional

fun <T> Optional<T>.toNullable(): T? = orElse(null)
