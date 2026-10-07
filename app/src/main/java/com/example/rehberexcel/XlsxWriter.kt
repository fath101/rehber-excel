package com.example.rehberexcel

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Harici kütüphane kullanmadan basit bir .xlsx dosyası üretir. */
object XlsxWriter {

    fun write(out: OutputStream, header: List<String>, rows: List<List<String>>) {
        ZipOutputStream(out).use { zip ->
            fun add(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            add("[Content_Types].xml", CONTENT_TYPES)
            add("_rels/.rels", ROOT_RELS)
            add("xl/workbook.xml", WORKBOOK)
            add("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            add("xl/styles.xml", STYLES)
            add("xl/worksheets/sheet1.xml", sheet(header, rows))
        }
    }

    private fun sheet(header: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<cols><col min="1" max="1" width="32" customWidth="1"/><col min="2" max="2" width="22" customWidth="1"/></cols>""")
        sb.append("<sheetData>")
        (listOf(header) + rows).forEachIndexed { r, row ->
            sb.append("""<row r="${r + 1}">""")
            row.forEachIndexed { c, value ->
                val ref = "${'A' + c}${r + 1}"
                val style = if (r == 0) """ s="1"""" else ""
                // Metin olarak yazılır: baştaki 0 ve + işaretleri korunur
                sb.append("""<c r="$ref" t="inlineStr"$style><is><t xml:space="preserve">${esc(value)}</t></is></c>""")
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch.code < 0x20 && ch != '\t' && ch != '\n' && ch != '\r' -> {} // geçersiz XML karakteri
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private const val XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

    private const val CONTENT_TYPES = XML +
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
        """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
        """<Default Extension="xml" ContentType="application/xml"/>""" +
        """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
        """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
        """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
        """</Types>"""

    private const val ROOT_RELS = XML +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        """</Relationships>"""

    private const val WORKBOOK = XML +
        """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
        """<sheets><sheet name="Rehber" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private const val WORKBOOK_RELS = XML +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
        """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
        """</Relationships>"""

    private const val STYLES = XML +
        """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
        """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
        """<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>""" +
        """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>""" +
        """</styleSheet>"""
}
