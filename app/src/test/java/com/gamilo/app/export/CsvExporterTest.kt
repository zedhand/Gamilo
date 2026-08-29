package com.gamilo.app.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExporterTest {

    @Test
    fun toCsv_joinsHeadersAndRowsWithCrlf() {
        val csv = CsvExporter.toCsv(listOf("a", "b"), listOf(listOf("1", "2"), listOf("3", "4")))
        assertEquals("a,b\r\n1,2\r\n3,4\r\n", csv)
    }

    @Test
    fun toCsv_quotesFieldsContainingCommasQuotesOrNewlines() {
        val csv = CsvExporter.toCsv(
            listOf("field"),
            listOf(
                listOf("plain"),
                listOf("has,comma"),
                listOf("has\"quote"),
                listOf("has\nnewline"),
            ),
        )
        val expected = "field\r\n" +
            "plain\r\n" +
            "\"has,comma\"\r\n" +
            "\"has\"\"quote\"\r\n" +
            "\"has\nnewline\"\r\n"
        assertEquals(expected, csv)
    }
}
