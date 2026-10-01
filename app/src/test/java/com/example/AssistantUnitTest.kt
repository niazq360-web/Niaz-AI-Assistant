package com.example

import com.example.agent.ToolRegistry
import com.example.data.model.ExecutionLevel
import com.example.data.model.Language
import com.example.data.model.LocalizationStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantUnitTest {

    @Test
    fun testToolRegistryIntegrations() {
        val tools = ToolRegistry.tools
        assertTrue(tools.isNotEmpty())

        // Verify required services exist
        val services = tools.map { it.service }.toSet()
        assertTrue(services.contains("Gmail"))
        assertTrue(services.contains("Google Calendar"))
        assertTrue(services.contains("Google Drive"))
        assertTrue(services.contains("WhatsApp"))
        assertTrue(services.contains("Facebook"))
        assertTrue(services.contains("Instagram"))
        assertTrue(services.contains("Canva"))
        assertTrue(services.contains("GitHub"))

        // Verify level 3 high-risk tools exist and require explicit confirmation
        val highRiskTools = tools.filter { it.level == ExecutionLevel.LEVEL_3_HIGH_RISK }
        assertTrue(highRiskTools.isNotEmpty())
    }

    @Test
    fun testLocalization() {
        val greetingEn = LocalizationStrings.getGreeting(Language.ENGLISH, "Niaz Ahmed")
        val greetingUr = LocalizationStrings.getGreeting(Language.URDU, "Niaz Ahmed")
        val greetingSd = LocalizationStrings.getGreeting(Language.SINDHI, "Niaz Ahmed")

        assertTrue(greetingEn.contains("Niaz Ahmed"))
        assertTrue(greetingUr.contains("Niaz Ahmed"))
        assertTrue(greetingSd.contains("Niaz Ahmed"))

        assertTrue(Language.URDU.isRtl)
        assertTrue(Language.SINDHI.isRtl)
        assertTrue(!Language.ENGLISH.isRtl)
    }

    @Test
    fun testToolRetrieval() {
        val gmailSend = ToolRegistry.getTool("gmail.send")
        assertNotNull(gmailSend)
        assertEquals(ExecutionLevel.LEVEL_2_CONFIRM, gmailSend?.level)

        val calDelete = ToolRegistry.getTool("calendar.delete")
        assertNotNull(calDelete)
        assertEquals(ExecutionLevel.LEVEL_3_HIGH_RISK, calDelete?.level)
    }
}
