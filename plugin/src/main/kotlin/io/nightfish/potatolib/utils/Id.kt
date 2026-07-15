package io.nightfish.potatolib.utils

import io.nightfish.lightnovelreader.api.identifier.Identifier

fun String.toId() = Identifier("potato_lib", this)