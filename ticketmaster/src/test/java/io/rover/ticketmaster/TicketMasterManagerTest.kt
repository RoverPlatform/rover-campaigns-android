package io.rover.ticketmaster

import io.rover.campaigns.ticketmaster.TicketmasterManager.Member
import junit.framework.Assert.assertEquals
import org.spekframework.spek2.Spek

private const val ID = "id"
private const val EMAIL = "example email"
private const val FIRST_NAME = "example first name"

object TicketMasterManagerTest: Spek({
    group("tmManagerMember") {
        test("getNonNullPropertiesMap ignores null properties") {
            val member = Member(ID, null, null)
            val expectedMap = mapOf(member::ticketmasterID.name to ID)

            assertEquals(expectedMap, member.getNonNullPropertiesMap())
        }

        test("getNonNullPropertiesMap returns all non null properties") {
            val member = Member(ID, EMAIL, FIRST_NAME)
            val expectedMap = mapOf(
                member::ticketmasterID.name to ID,
                member::email.name to EMAIL,
                member::firstName.name to FIRST_NAME
            )

            assertEquals(expectedMap, member.getNonNullPropertiesMap())
        }

        test("getNonNullPropertiesMap returns empty with all null properties") {
            val member = Member(null, null, null)
            val expectedMap = mapOf<String, String>()

            assertEquals(expectedMap, member.getNonNullPropertiesMap())
        }
    }
})