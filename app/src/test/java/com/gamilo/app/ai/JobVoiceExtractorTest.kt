package com.gamilo.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JobVoiceExtractorTest {

    @Test
    fun extract_parsesClientAndTitle_fromNewJobForXToYPhrasing() {
        val draft = JobVoiceExtractor.extract("new job for Jane Smith to replace the kitchen faucet")
        assertEquals("Jane Smith", draft.clientName)
        assertEquals("replace the kitchen faucet", draft.title)
    }

    @Test
    fun extract_dropsTrailingAppointmentClause_fromBothFieldsButKeepsRawText() {
        val text = "new job for Jane Smith to replace faucet. appointment booked for tuesday july 9th at 11am"
        val draft = JobVoiceExtractor.extract(text)
        assertEquals("Jane Smith", draft.clientName)
        assertEquals("replace faucet", draft.title)
        assertEquals(text, draft.rawText)
    }

    @Test
    fun extract_handlesJobForWithoutNewPrefix() {
        val draft = JobVoiceExtractor.extract("job for Bob Jones to fix the drywall")
        assertEquals("Bob Jones", draft.clientName)
        assertEquals("fix the drywall", draft.title)
    }

    @Test
    fun extract_fallsBackToClientOnlyWhenNoToClause() {
        val draft = JobVoiceExtractor.extract("new job for Sarah Connor")
        assertEquals("Sarah Connor", draft.clientName)
        assertNull(draft.title)
    }

    @Test
    fun extract_fallsBackToTitleOnlyWhenNoForClause() {
        val draft = JobVoiceExtractor.extract("replace the garbage disposal")
        assertNull(draft.clientName)
        assertEquals("replace the garbage disposal", draft.title)
    }

    @Test
    fun extract_blankInput_returnsAllNullFields() {
        val draft = JobVoiceExtractor.extract("   ")
        assertNull(draft.clientName)
        assertNull(draft.title)
    }

    @Test
    fun extract_isCaseInsensitiveOnKeywords() {
        val draft = JobVoiceExtractor.extract("NEW JOB FOR Jane Smith TO replace the faucet")
        assertEquals("Jane Smith", draft.clientName)
        assertEquals("replace the faucet", draft.title)
    }
}
