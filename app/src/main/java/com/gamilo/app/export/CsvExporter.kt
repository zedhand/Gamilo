package com.gamilo.app.export

/**
 * RFC 4180 CSV writer. A field is quoted whenever it contains a comma, quote, CR, or LF;
 * embedded quotes are doubled.
 */
object CsvExporter {

    fun toCsv(headers: List<String>, rows: List<List<String>>): String {
        val builder = StringBuilder()
        builder.append(headers.joinToString(",") { quoteIfNeeded(it) }).append("\r\n")
        for (row in rows) {
            builder.append(row.joinToString(",") { quoteIfNeeded(it) }).append("\r\n")
        }
        return builder.toString()
    }

    private fun quoteIfNeeded(field: String): String {
        val needsQuoting = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) "\"${field.replace("\"", "\"\"")}\"" else field
    }
}
