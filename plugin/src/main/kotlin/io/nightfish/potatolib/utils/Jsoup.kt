package io.nightfish.potatolib.utils

import org.jsoup.nodes.Document

fun Document.selectSingleXPath(path: String) = this.selectXpath(path).firstOrNull()