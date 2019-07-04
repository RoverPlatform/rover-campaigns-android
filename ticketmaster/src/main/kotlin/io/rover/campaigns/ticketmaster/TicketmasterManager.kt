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

private const val TICKETMASTER_ID_KEY = "id"

class TicketmasterManager(
    private val applicationContext: Context,
    private val userInfo: UserInfoInterface,
    localStorage: LocalStorage
) : TicketmasterAuthorizer, SyncParticipant {
    private val storage = localStorage.getKeyValueStorageFor(STORAGE_CONTEXT_IDENTIFIER)

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "ticketmaster"

        private const val LEGACY_STORAGE_2X_SHARED_PREFERENCES = "io.rover.core.platform.localstorage.io.rover.ticketmaster.TicketmasterManager"
    }

    override fun setCredentials(backendNameOrdinal: Int, memberId: String?) {
        member = Member(
            id = memberId,
            email = null,
            firstName = null
        )
    }

    override fun setCredentials(id: String, email: String?, firstName: String?) {
        member = Member(
            id = id,
            email = email,
            firstName = firstName
        )

        val localPropertiesMap = member?.getNonNullPropertiesMap()

        userInfo.update {
            val tmAttributes = it.getValue("ticketmaster") as MutableMap<String, Any>
            if (it.containsKey("ticketmaster")) {
                localPropertiesMap?.forEach { (propertyName, propertyValue) -> tmAttributes[propertyName] = propertyValue }
                it["ticketmaster"] = tmAttributes
        } else {
            if(localPropertiesMap?.isNotEmpty() == true) it["ticketmaster"] = localPropertiesMap
        } }
    }

    override fun clearCredentials() {
        member = null
        userInfo.update { it.remove("ticketmaster") }
    }

    override fun initialRequest(): SyncRequest? {
        return member.whenNotNull { member ->

            val params = listOfNotNull(
                member.id.whenNotNull { Pair(TICKETMASTER_ID_KEY, it) }
            )

            if (params.isEmpty()) {
                null
            } else {
                SyncRequest(
                    SyncQuery.ticketmaster,
                    variables = params.associate { it }
                )
            }
        }
    }

    private fun <K, V> MutableMap<K, V>.putIfNotPresent(keyToPut: K, valueToPut: V) {
        if (!this.containsKey(keyToPut)) this[keyToPut] = valueToPut
    }

    override fun saveResponse(json: JSONObject): SyncResult {
        return try {
            val profileAttributesFromNetwork = TicketmasterSyncResponseData.decodeJson(json.getJSONObject("data")).ticketmasterProfile.attributes
            val localMemberProperties = member?.getNonNullPropertiesMap()
            val mutableMapFromNetwork = profileAttributesFromNetwork?.toMutableMap()

            localMemberProperties?.forEach {(key, value) ->
                mutableMapFromNetwork?.put(key, value)
            }

            mutableMapFromNetwork.whenNotNull { attributes ->
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

        if (legacyTicketmasterMemberJson != null) {
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
        val id: String?,
        val email: String?,
        val firstName: String?
    ) {
        companion object

        fun getNonNullPropertiesMap(): Map<String, String> {
            val propertiesMap = mutableMapOf<String, String>()

            id.whenNotNull { propertiesMap.put(TicketmasterManager.Member::id.name, it) }
            email.whenNotNull { propertiesMap.put(TicketmasterManager.Member::email.name, it) }
            firstName.whenNotNull { propertiesMap.put(TicketmasterManager.Member::firstName.name, it) }
            return propertiesMap
        }
    }
}

val SyncQuery.Argument.Companion.id
    get() = SyncQuery.Argument(TICKETMASTER_ID_KEY, "String")

val SyncQuery.Companion.ticketmaster
    get() = SyncQuery(
        "ticketmasterProfile",
        """
            attributes
        """.trimIndent(),
        listOf(SyncQuery.Argument.id),
        listOf()
    )

fun TicketmasterManager.Member.Companion.decodeJson(json: JSONObject): TicketmasterManager.Member {
    return TicketmasterManager.Member(
        id = json.safeOptString(TicketmasterManager.Member::id.name),
        email = json.safeOptString(TicketmasterManager.Member::email.name),
        firstName = json.safeOptString(TicketmasterManager.Member::firstName.name)
    )
}

fun TicketmasterManager.Member.encodeJson(): JSONObject {
    return JSONObject().apply {
        listOf(TicketmasterManager.Member::id, TicketmasterManager.Member::email, TicketmasterManager.Member::firstName).forEach {
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
