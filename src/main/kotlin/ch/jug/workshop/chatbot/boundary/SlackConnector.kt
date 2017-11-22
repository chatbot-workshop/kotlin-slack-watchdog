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

package ch.jug.workshop.chatbot.boundary

import ch.jug.workshop.chatbot.control.WatchdogService
import ch.jug.workshop.chatbot.entity.Status
import ch.jug.workshop.chatbot.entity.Website
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import java.util.*

class SlackConnector : Connector {

    private val session: SlackSession
    private val channel: SlackChannel
    private val watchdogService: WatchdogService

    init {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("bot.conf")
        val conf = Properties()
        conf.load(stream)
        val authToken = conf.getProperty("authToken")
        val channelName = conf.getProperty("channelName")

        val messagePostedListener = SlackMessagePostedListener { event, session ->
            analyzeMessage(event)
        }

        session = SlackSessionFactory.createWebSocketSlackSession(authToken)
        session.connect()
        channel = session.findChannelByName(channelName)
        session.addMessagePostedListener(messagePostedListener)

        watchdogService = WatchdogService(this)
    }

    private fun analyzeMessage(event: SlackMessagePosted) {
        val message = event.messageContent.toLowerCase().trim()
        if (message.startsWith("start monitoring")) {
            startMonitoring(event)
        } else if (message.startsWith("stop monitoring")) {
            stopMonitoring(event)
        } else if (message.startsWith("show status")) {
            showStatus(event, session)
        }
    }

    private fun startMonitoring(event: SlackMessagePosted) {
        val message = event.messageContent.trim()
        val address = message.split(" ").get(2).trim().drop(1).dropLast(1)
        val website = Website(address, Status.UNKNOWN)
        watchdogService.startMonitoring(website)
        session.sendMessage(event.channel, "Okay, I'm going to monitor '${address}'")
    }

    private fun stopMonitoring(event: SlackMessagePosted) {
        val message = event.messageContent.trim()
        val address = message.split(" ").get(2).trim().drop(1).dropLast(1)
        val website = Website(address, Status.UNKNOWN)
        watchdogService.stopMonitoring(website)
        session.sendMessage(event.channel, "I just stopped monitoring '${address}'")
    }

    private fun showStatus(event: SlackMessagePosted, session: SlackSession) {
        val message = event.messageContent.trim()
        val address = message.split(" ").get(3).trim().drop(1).dropLast(1)
        val website = Website(address, Status.UNKNOWN)
        val status = watchdogService.status(website)
        session.sendMessage(event.channel, "The status of the website '${address}' is '${status}'")
    }

    override fun sendMessage(message: String) {
        session.sendMessage(channel, message)
    }

}
