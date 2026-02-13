package io.nightfish.potatolib.utils

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

fun Document.selectSingleXPath(path: String) = this.selectXpath(path).firstOrNull()
fun Element.selectSingleXPath(path: String) = this.selectXpath(path).firstOrNull()