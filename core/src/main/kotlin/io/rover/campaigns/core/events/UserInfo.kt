package io.rover.campaigns.core.events

import io.rover.campaigns.core.data.domain.Attributes
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
        val previousTags = mutableDraft["tags"]
        builder(mutableDraft)

        if (mutableDraft["tags"] != previousTags) {
            throw RuntimeException("Can not modify tags inside an update block. Please call addTag or removeTag")
        }

        currentUserInfo = mutableDraft
    }

    override fun addTag(tag: String, expiresInSeconds: Long?) {
        currentTags = currentTags.add(tag, expiresInSeconds?.let { Date(Date().time + it * 1000) })
    }

    override fun removeTag(tag: String) {
        currentTags = currentTags.remove(tag)
    }

    override fun clear() {
        currentUserInfo = hashMapOf()
        currentTags = TagSet.empty()
    }

    override var currentUserInfo: Attributes = try {
        when (val attributes = store[USER_INFO_KEY]) {
            null -> hashMapOf()
            else -> {
                val info = JSONObject(attributes).toAttributesHash().toMutableMap()
                if (info.containsKey("tags")) {
                    info["tags"]?.let { migrateTags(it) }
                    info.remove("tags")
                    store[USER_INFO_KEY] = info.encodeJson().toString()
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
            mutableInfo["tags"] = currentTags.values()
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

    private var currentTags: TagSet = try {
        when (val data = store[TAGS_KEY]) {
            null -> TagSet.empty()
            else -> TagSet.decodeJson(data).filterActiveTags()
        }
    } catch (throwable: Throwable) {
        log.w("Corrupted local tags, ignoring and starting fresh. Cause ${throwable.message}")
        TagSet.empty()
    }
        get() = field.filterActiveTags()
        set(value) {
            field = value.filterActiveTags()
            store[TAGS_KEY] = field.encodeJson()
        }

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "user-info"
        private const val USER_INFO_KEY = "user-info"
        private const val TAGS_KEY = "tags"
    }
}

private data class TagSet(
    private val data: Map<String, Date?>
) {

    fun add(tag: String, expires: Date?): TagSet {
        val mutableData = data.toMutableMap()
        mutableData[tag] = expires
        return TagSet(data = mutableData)
    }

    fun remove(tag: String): TagSet {
        val mutableData = data.toMutableMap()
        mutableData.remove(tag)
        return TagSet(data = mutableData)
    }

    fun contains(tag: String): Boolean = data.containsKey(tag)

    fun values(): List<String> = data.keys.toList()

    fun filterActiveTags(): TagSet = TagSet(data = data.filter {
        when (val value = it.value) {
            null -> true
            else -> Date().before(value)
        }
    })

    fun encodeJson(): String {
        return JSONObject(data.map {
            val (key, value) = it
            when (value) {
                null -> Pair(key, JSONObject.NULL)
                else -> Pair(key, value.time)
            }
        }.associate { it }).toString()
    }

    companion object {
        fun decodeJson(input: String): TagSet {
            val json = JSONObject(input)
            val data = json.keys().asSequence().map {
                val value = json.get(it)
                val expires = if (value is Long) Date(value) else null
                Pair(it, expires)
            }.associate { it }.toMap()

            return TagSet(data = data)
        }

        fun empty(): TagSet = TagSet(data = emptyMap())
    }
}