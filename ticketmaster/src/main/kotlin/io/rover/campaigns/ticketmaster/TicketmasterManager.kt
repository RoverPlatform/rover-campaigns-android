package io.rover.campaigns.ticketmaster

import android.content.Context
import android.os.Build
import io.rover.campaigns.core.data.domain.Attributes
import io.rover.campaigns.core.data.graphql.operations.data.toAttributesHash
import io.rover.campaigns.core.data.graphql.putProp
import io.rover.campaigns.core.data.graphql.safeOptString
import io.rover.campaigns.core.data.sync.SyncParticipant
import io.rover.campaigns.core.data.sync.SyncQuery
import io.rover.campaigns.core.data.sync.SyncRequest
import io.rover.campaigns.core.data.sync.SyncResult
import io.rover.campaigns.core.events.UserInfoInterface
import io.rover.campaigns.core.logging.log
import io.rover.campaigns.core.platform.LocalStorage
import io.rover.campaigns.core.platform.whenNotNull
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException

class TicketmasterManager(
    private val applicationContext: Context,
    private val userInfo: UserInfoInterface,
    localStorage: LocalStorage
) : TicketmasterAuthorizer, SyncParticipant {
    private val storage = localStorage.getKeyValueStorageFor(STORAGE_CONTEXT_IDENTIFIER)

    override fun setCredentials(backendNameOrdinal: Int, memberId: String?) {

        val backendName = TicketmasterBackendName.values()[backendNameOrdinal]

        member = Member(
            hostID = if(backendName == TicketmasterBackendName.HOST) memberId else null,
            teamID = if(backendName == TicketmasterBackendName.ARCHTICS) memberId else null
        )
    }

    override fun clearCredentials() {
        member = null
        userInfo.update { it.remove("ticketmaster") }
    }

    override fun initialRequest(): SyncRequest? {
        return member.whenNotNull { member ->

            val params = listOfNotNull(
                member.hostID.whenNotNull { Pair("hostMemberID", it) },
                member.teamID.whenNotNull { Pair("teamMemberID", it) }
            )

            if(params.isEmpty()) {
                null
            } else {
                SyncRequest(
                    SyncQuery.ticketmaster,
                    variables = params.associate { it }
                )
            }
        }
    }

    override fun saveResponse(json: JSONObject): SyncResult {
        return try {
            val profileAttributes = TicketmasterSyncResponseData.decodeJson(json.getJSONObject("data")).ticketmasterProfile.attributes

            profileAttributes.whenNotNull { attributes ->
                userInfo.update { userInfo ->
                    userInfo["ticketmaster"] = attributes
                }
                SyncResult.NewData(null)
            } ?: SyncResult.NoData
        } catch (e: JSONException) {
            log.v("Unable to parse ticketmaster profile data from Rover API, ignoring: $e")
            SyncResult.Failed
        }
    }

    private var member: Member?
        get() {
            val storageJson = storage["member"] ?: getAndClearSdk2TicketmasterIdentifierIfPresent()
            return storageJson.whenNotNull { memberString ->
                try {
                    Member.decodeJson(JSONObject(memberString))
                } catch (e: JSONException) {
                    log.w("Invalid JSON in ticketmaster manager storage, ignoring: $e")
                    null
                }
            }
        }
        set(value) {
            storage["member"] = value?.encodeJson().toString()
        }

    private fun getAndClearSdk2TicketmasterIdentifierIfPresent(): String? {
        val legacySharedPreferences = applicationContext.getSharedPreferences(
            LEGACY_STORAGE_2X_SHARED_PREFERENCES,
            Context.MODE_PRIVATE
        )

        val legacyTicketmasterMemberJson = legacySharedPreferences.getString(
            "member",
            null
        )

        if(legacyTicketmasterMemberJson != null) {
            log.i("Migrated legacy Rover SDK 2.x Ticketmaster member data.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    log.v("Deleting legacy shared preferences file.")
                    applicationContext.deleteSharedPreferences(LEGACY_STORAGE_2X_SHARED_PREFERENCES)
                } catch (e: FileNotFoundException) {
                    log.w("Unable to delete legacy Rover shared preferences file: $e")
                }
            }
        }
        return legacyTicketmasterMemberJson
    }

    data class Member(
        val hostID: String?,
        val teamID: String?
    ) {
        companion object
    }

    enum class TicketmasterBackendName {
        HOST, ARCHTICS
    }

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "ticketmaster"

        private const val LEGACY_STORAGE_2X_SHARED_PREFERENCES = "io.rover.core.platform.localstorage.io.rover.ticketmaster.TicketmasterManager"
    }
}

val SyncQuery.Argument.Companion.hostMemberId
    get() = SyncQuery.Argument("hostMemberID", "String")

val SyncQuery.Argument.Companion.teamMemberID
    get() = SyncQuery.Argument("teamMemberID", "String")


val SyncQuery.Companion.ticketmaster
    get() = SyncQuery(
        "ticketmasterProfile",
        """
            attributes
        """.trimIndent(),
        listOf(SyncQuery.Argument.hostMemberId, SyncQuery.Argument.teamMemberID),
        listOf()
    )

fun TicketmasterManager.Member.Companion.decodeJson(json: JSONObject): TicketmasterManager.Member {
    return TicketmasterManager.Member(
        hostID = json.safeOptString("hostID"),
        teamID = json.safeOptString("teamID")
    )
}

fun TicketmasterManager.Member.encodeJson(): JSONObject {
    return JSONObject().apply {
        listOf(TicketmasterManager.Member::hostID, TicketmasterManager.Member::teamID).forEach {
            putProp(this@encodeJson, it)
        }
    }
}

class TicketmasterSyncResponseData(
    val ticketmasterProfile: Profile
) {
    data class Profile(
        val attributes: Attributes?
    ) {
        companion object
    }

    companion object
}

fun TicketmasterSyncResponseData.Companion.decodeJson(json: JSONObject): TicketmasterSyncResponseData {
    return TicketmasterSyncResponseData(
        TicketmasterSyncResponseData.Profile.decodeJson(json.getJSONObject("ticketmasterProfile"))
    )
}

fun TicketmasterSyncResponseData.Profile.Companion.decodeJson(json: JSONObject): TicketmasterSyncResponseData.Profile {
    return TicketmasterSyncResponseData.Profile(
        json.optJSONObject("attributes")?.toAttributesHash()
    )
}

