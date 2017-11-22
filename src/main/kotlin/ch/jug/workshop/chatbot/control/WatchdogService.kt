/**
 * Kotlin Slack Watchdog Bot - A more complex bot to monitor websites
 * Copyright (C) 2017 Marcus Fihlon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.jug.workshop.chatbot.control

import ch.jug.workshop.chatbot.boundary.Connector
import ch.jug.workshop.chatbot.entity.Status
import ch.jug.workshop.chatbot.entity.Website
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.time.LocalDateTime
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.concurrent.timer

class WatchdogService(val connector: Connector) {

    private val websites: MutableSet<Website> = mutableSetOf()

    init {
        timer("Watchdog", true, 60000, 60000, { checkAllWebsites() })
    }

    fun startMonitoring(website: Website) {
        websites.add(website)
        Timer().schedule(1000, {checkWebsite(website)})
    }

    fun stopMonitoring(website: Website) {
        websites.remove(website)
    }

    fun status(website: Website): Status = websites.parallelStream()
            .filter { w -> w.address == website.address }
            .findAny()
            .map { w -> w.status }
            .orElse(Status.UNKNOWN)

    private fun checkAllWebsites() {
        websites.parallelStream().forEach { website ->
            checkWebsite(website)
        }
    }

    private fun checkWebsite(website: Website) {
        website.address.httpGet().response { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    updateStatus(website, Status.DOWN)
                }
                is Result.Success -> {
                    updateStatus(website, Status.UP)
                }
            }
        }
    }

    private fun updateStatus(website: Website, status: Status) {
        if (website.status != status) {
            val time = LocalDateTime.now()
            val message = "The website '${website.address}' was '${website.status}' and is now '${status}' (${time})."
            connector.sendMessage(message)
            website.status = status
        }
    }

}
