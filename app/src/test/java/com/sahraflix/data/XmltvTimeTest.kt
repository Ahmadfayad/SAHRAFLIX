package com.sahraflix.data

import com.sahraflix.data.repository.XmltvTime
import org.junit.Assert.assertEquals
import org.junit.Test

class XmltvTimeTest {
    @Test fun parsesAllCommonForms() {
        assertEquals(1_700_000_000_000L, XmltvTime.parse("20231114221320 +0000"))
        assertEquals(1_700_000_000_000L, XmltvTime.parse("20231114231320+0100"))
        assertEquals(1_700_000_000_000L, XmltvTime.parse("20231114221320"))
        assertEquals(1_699_999_980_000L, XmltvTime.parse("202311142213"))
    }

    @Test fun invalidIsZero() {
        assertEquals(0L, XmltvTime.parse("garbage"))
        assertEquals(0L, XmltvTime.parse(null))
    }
}
