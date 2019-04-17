package io.rover.campaigns.location.domain.events

import io.rover.campaigns.core.data.domain.AttributeValue
import io.rover.campaigns.location.domain.Beacon

fun Beacon.asAttributeValue(): AttributeValue {
    return AttributeValue.Object(
        Pair("id", AttributeValue.Scalar.String(id.rawValue)),
        Pair("name", AttributeValue.Scalar.String(name)),
        Pair("uuid", AttributeValue.Scalar.String(uuid.toString())),
        Pair("major", AttributeValue.Scalar.Integer(major)),
        Pair("minor", AttributeValue.Scalar.Integer(minor)),
        Pair("name", AttributeValue.Scalar.String(name)),
        Pair("tags", AttributeValue.Array(
            tags.map { AttributeValue.Scalar.String(it )}
        ))
    )
}
