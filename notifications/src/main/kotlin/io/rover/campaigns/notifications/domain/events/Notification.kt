package io.rover.campaigns.notifications.domain.events

import io.rover.campaigns.core.data.domain.AttributeValue
import io.rover.campaigns.notifications.domain.Notification

fun Notification.asAttributeValue(): AttributeValue {
    return AttributeValue.Object(
        Pair("id", AttributeValue.Scalar.String((id))),
        Pair("campaignID", AttributeValue.Scalar.String(campaignId))
    )
}