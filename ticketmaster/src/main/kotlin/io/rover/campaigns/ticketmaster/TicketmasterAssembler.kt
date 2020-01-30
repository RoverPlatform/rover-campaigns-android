package io.rover.campaigns.ticketmaster

import android.app.Application
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.rover.campaigns.core.RoverCampaigns
import io.rover.campaigns.core.container.Assembler
import io.rover.campaigns.core.container.Container
import io.rover.campaigns.core.container.Resolver
import io.rover.campaigns.core.container.Scope
import io.rover.campaigns.core.data.sync.SyncCoordinatorInterface
import io.rover.campaigns.core.data.sync.SyncParticipant
import io.rover.campaigns.core.events.UserInfoInterface
import io.rover.campaigns.core.platform.LocalStorage

private const val MY_TICKET_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.MYTICKETSCREENSHOWED"
private const val MANAGE_TICKET_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.MANAGETICKETSCREENSHOWED"
private const val ADD_PAYMENT_INFO_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.ADDPAYMENTINFOSCREENSHOWED"
private const val MY_TICKET_BARCODE_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.MYTICKETBARCODESCREENSHOWED"
private const val TICKET_DETAIL_SCREEN_SHOWED = "com.ticketmaster.presencesdk.eventanalytic.action.TICKETDETAILSSCREENSHOWED"

class TicketmasterAssembler : Assembler {
    override fun assemble(container: Container) {
        container.register(
            Scope.Singleton,
            TicketmasterAuthorizer::class.java
        ) { resolver ->
            resolver.resolveSingletonOrFail(TicketmasterManager::class.java)
        }

        container.register(
            Scope.Singleton,
            TicketmasterManager::class.java
        ) { resolver ->
            TicketmasterManager(
                resolver.resolveSingletonOrFail(Application::class.java),
                resolver.resolveSingletonOrFail(UserInfoInterface::class.java),
                resolver.resolveSingletonOrFail(LocalStorage::class.java)
            )
        }

        container.register(
            Scope.Singleton,
            SyncParticipant::class.java,
            "ticketmaster"
        ) { resolver -> resolver.resolveSingletonOrFail(TicketmasterManager::class.java) }
    }

    override fun afterAssembly(resolver: Resolver) {
        resolver.resolveSingletonOrFail(SyncCoordinatorInterface::class.java).registerParticipant(
            resolver.resolveSingletonOrFail(
                SyncParticipant::class.java,
                "ticketmaster"
            )
        )

        val analyticEventFilter = IntentFilter().apply {
            addAction(MY_TICKET_SCREEN_SHOWED)
            addAction(MANAGE_TICKET_SCREEN_SHOWED)
            addAction(ADD_PAYMENT_INFO_SCREEN_SHOWED)
            addAction(MY_TICKET_BARCODE_SCREEN_SHOWED)
            addAction(TICKET_DETAIL_SCREEN_SHOWED)
        }

        LocalBroadcastManager.getInstance(resolver.resolveSingletonOrFail(Application::class.java).applicationContext)
            .registerReceiver(TicketMasterAnalyticsBroadcastReceiver(), analyticEventFilter)
    }
}

@Deprecated("Use .resolve(TicketmasterAuthorizer::class.java)")
val RoverCampaigns.ticketmasterAuthorizer: TicketmasterAuthorizer
    get() = RoverCampaigns.sharedInstance.resolveSingletonOrFail(TicketmasterAuthorizer::class.java)
