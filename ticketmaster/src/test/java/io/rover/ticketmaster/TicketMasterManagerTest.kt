package io.rover.ticketmaster

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.rover.campaigns.ticketmaster.TicketmasterManager.Member
import io.rover.campaigns.ticketmaster.encodeJson
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.spekframework.spek2.Spek

private const val ID = "id"
private const val EMAIL = "example email"
private const val FIRST_NAME = "example first name"

object TicketMasterManagerTest: Spek({
    group("tmManagerMember") {
        test("getNonNullPropertiesMap() ignores null properties") {
            val member = Member(ID, null, null)
            val expectedMap = mapOf(member::ticketmasterID.name to ID)

            assertEquals(expectedMap, member.getNonNullPropertiesMap())
        }

        test("getNonNullPropertiesMap() returns all non null properties") {
            val member = Member(ID, EMAIL, FIRST_NAME)
            val expectedMap = mapOf(
                member::ticketmasterID.name to ID,
                member::email.name to EMAIL,
                member::firstName.name to FIRST_NAME
            )

            assertEquals(expectedMap, member.getNonNullPropertiesMap())
        }

        test("getNonNullPropertiesMap() returns empty with all null properties") {
            val member = Member(null, null, null)
            val expectedMap = mapOf<String, String>()

            assertEquals(expectedMap, member.getNonNullPropertiesMap())
        }

        test("encodeJson() adds properties to JSONObject") {
            val member = Member(ID, EMAIL, FIRST_NAME)
            val mockJsonObject = mock<JSONObject>()

            member.encodeJson(mockJsonObject)

            verify(mockJsonObject).put(member::ticketmasterID.name, ID)
            verify(mockJsonObject).put(member::firstName.name, FIRST_NAME)
            verify(mockJsonObject).put(member::email.name, EMAIL)
        }
    }
})