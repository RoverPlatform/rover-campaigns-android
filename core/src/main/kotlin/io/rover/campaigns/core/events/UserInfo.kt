package io.rover.campaigns.core.events

import io.rover.campaigns.core.data.domain.Attributes
import io.rover.campaigns.core.data.graphql.getObjectIterable
import io.rover.campaigns.core.data.graphql.operations.data.encodeJson
import io.rover.campaigns.core.data.graphql.operations.data.toAttributesHash
import io.rover.campaigns.core.logging.log
import io.rover.campaigns.core.platform.LocalStorage
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

class UserInfo(
    localStorage: LocalStorage
) : UserInfoInterface {
    private val store = localStorage.getKeyValueStorageFor(STORAGE_CONTEXT_IDENTIFIER)

    override fun update(builder: (attributes: HashMap<String, Any>) -> Unit) {
        val mutableDraft = HashMap(currentUserInfo)
        builder(mutableDraft)
        if (mutableDraft.containsKey("tags")) {
            if (mutableDraft["tags"] is Collection<*>) {
                val mutatedTags =
                    (mutableDraft["tags"] as Collection<*>).filterIsInstance<String>().toSet()

                val addedTags =
                    mutatedTags.filter { !currentTags.contains(Tag(value = it, expires = null)) }
                val removedTags = currentTags.filter { !mutatedTags.contains(it.value) }

                if (addedTags.isNotEmpty()) {
                    log.w("Deprecation notice: Attempted to add tags directly instead of using addTag")
                    addedTags.forEach { addTag(it) }
                }
                if (removedTags.isNotEmpty()) {
                    log.w("Deprecation notice: Attempted to remove tags directly instead of using removeTag")
                    removedTags.forEach { removeTag(it.value) }
                }
            } else {
                log.w("Attempted updating tags to a non collection type")
                mutableDraft.remove("tags")
            }
        }
        currentUserInfo = mutableDraft
    }

    override fun addTag(tag: String) {
        addTag(tag, null)
    }

    override fun addTag(tag: String, expiresInSeconds: Long?) {
        val expires = if (expiresInSeconds != null) {
            Date(Date().time + expiresInSeconds)
        } else null

        val mutableTags = currentTags.toMutableSet()
        mutableTags.add(
            Tag(
                value = tag,
                expires = expires
            )
        )

        currentTags = mutableTags
    }

    override fun removeTag(tag: String) {
        val mutableTags = currentTags.toMutableSet()
        mutableTags.remove(Tag(value = tag, expires = null))
        currentTags = mutableTags
    }

    override fun tags(): Set<String> {
        return currentTags.map { it.value }.toSet()
    }

    override fun clear() {
        currentUserInfo = hashMapOf()
        currentTags = setOf()
    }

    private var currentTags: Set<Tag> = try {
        when (store[TAGS_KEY]) {
            null -> setOf()
            else -> decodeTags(store[TAGS_KEY]!!)
        }
    } catch (throwable: Throwable) {
        log.w("Corrupted local tags, ignoring and starting fresh. Cause ${throwable.message}")
        setOf()
    }
        get() {
            val now = Date()
            val unExpiredTags = field.filter {
                when (it.expires) {
                    null -> true
                    else -> it.expires.after(now)
                }
            }.toSet()

            if (unExpiredTags != field) {
                // Some tags have expired update the value
                currentTags = unExpiredTags
            }
            return unExpiredTags
        }
        set(value) {
            field = value
            store[TAGS_KEY] = encodeTags(value)
        }

    override var currentUserInfo: Attributes = try {
        when (store[USER_INFO_KEY]) {
            null -> hashMapOf()
            else -> {
                val info = JSONObject(store[USER_INFO_KEY]).toAttributesHash().toMutableMap()
                if (info.containsKey("tags")) {
                    log.v("Migrating user info tags")
                    info["tags"]?.let { migrateTags(it) }
                    info.remove("tags")
                    store[USER_INFO_KEY] = info.encodeJson().toString()
                    log.v("Migrated user info tags successfully")
                }
                info
            }
        }
    } catch (throwable: Throwable) {
        log.w("Corrupted local user info, ignoring and starting fresh.  Cause: ${throwable.message}")
        hashMapOf()
    }
        get() {
            val mutableInfo = HashMap(field)
            mutableInfo["tags"] = tags()
            return mutableInfo
        }
        private set(value) {
            field = value
            // Ensure we do not store tags in storage
            store[USER_INFO_KEY] = field.filterKeys { it != "tags" }.encodeJson().toString()
            log.v("Stored new user info.")
        }

    private fun migrateTags(tags: Any) {
        if (tags is JSONArray) {
            for (i in 0 until tags.length()) {
                val item = tags.optString(i)
                addTag(item)
            }
        }
    }

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "user-info"
        private const val USER_INFO_KEY = "user-info"
        private const val TAGS_KEY = "tags"
    }
}

private fun encodeTags(tags: Set<Tag>): String = JSONArray(tags.map { it.encodeJSON() }).toString()

private fun decodeTags(value: String): Set<Tag> =
    JSONArray(value).getObjectIterable().map { Tag.decodeJSON(it) }.toSet()

private data class Tag(
    val value: String,
    val expires: Date?

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tag

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    companion object
}

private fun Tag.encodeJSON(): JSONObject {
    return JSONObject().apply {
        put("value", value)
        expires?.let { put("expires", it.time) }
    }
}

private fun Tag.Companion.decodeJSON(json: JSONObject): Tag {
    return Tag(
        value = json.getString("value"),
        expires = if (json.has("expires")) Date(json.optLong("expires")) else null
    )
}




