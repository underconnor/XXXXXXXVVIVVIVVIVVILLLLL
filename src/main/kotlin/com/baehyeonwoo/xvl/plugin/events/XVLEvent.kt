/*
 * Copyright (c) 2021 BaeHyeonWoo
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baehyeonwoo.xvl.plugin.events

import com.baehyeonwoo.xvl.plugin.XVLPluginMain
import com.baehyeonwoo.xvl.plugin.enums.DecreaseReason
import com.baehyeonwoo.xvl.plugin.objects.XVLGameContentManager.ending
import com.baehyeonwoo.xvl.plugin.objects.XVLGameContentManager.manageFlags
import com.baehyeonwoo.xvl.plugin.objects.XVLGameContentManager.thirstValue
import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import io.github.monun.tap.effect.playFirework
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.md_5.bungee.api.ChatColor
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random.Default.nextInt

/***
 * @author BaeHyeonWoo
 */

class XVLEvent : Listener {
    private fun getInstance(): Plugin {
        return XVLPluginMain.instance
    }

    private fun decreaseThirst(player: Player, decreaseReason: DecreaseReason) {
        if (player.thirstValue < 600) {
            player.removePotionEffect(PotionEffectType.SLOW)
        } else if (player.thirstValue < 3600) {
            player.removePotionEffect(PotionEffectType.SLOW)
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 1000000, 0, true, false))
        } else if (player.thirstValue < 7200) {
            player.removePotionEffect(PotionEffectType.CONFUSION)
            player.removePotionEffect(PotionEffectType.SLOW)
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 1000000, 2, true, false))
        }

        if (decreaseReason == DecreaseReason.POTION) {
            if (player.thirstValue - 300 > 0) player.thirstValue = player.thirstValue - 300
            else if (player.thirstValue - 300 <= 0) player.thirstValue = 0
        }
        if (decreaseReason == DecreaseReason.MILK) {
            if (player.thirstValue - 150 > 0) player.thirstValue = player.thirstValue - 150
            else if (player.thirstValue - 150 <= 0) player.thirstValue = 0
        }
    }

    private val server = getInstance().server

    // No Damage Ticks to 0
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val p = e.player

        p.thirstValue = getInstance().config.getInt("${p.name}.thirstvalue", p.thirstValue)

        e.joinMessage(null)
        p.noDamageTicks = 0
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val p = e.player
        e.quitMessage(null)
        getInstance().config.set("${p.name}.thirstvalue", p.thirstValue)
        getInstance().saveConfig()

        manageFlags(FreezingFlag = false, ThirstyFlag = false, WarmBiomeFlag = false, NetherBiomeFlag = false)
    }

    // Nerfed from original LVX; Returning damage is set to 1.5x, which is lower than original.
    @EventHandler
    fun onEntityDamageByEntityEvent(e: EntityDamageByEntityEvent) {
        val dmgr = e.damager
        val dmg = e.finalDamage

        if (dmgr is Projectile) {
            if (dmgr.shooter is Player) {
                (dmgr.shooter as Player).damage((dmg * 1.5))
            } else return
        }

        if (dmgr is Player) {
            dmgr.damage(dmg * 1.5)
        }

        if (dmgr is Monster) {
            e.damage = e.damage * 1.5
        }
    }

    // Bed Event
    @EventHandler
    fun onPlayerBedLeave(e: PlayerBedLeaveEvent) {
        val p = e.player

        if (p.world.time == 0L) {
            when (nextInt(7)) {
                0 -> {
                    p.sendMessage(text("잠을 매우 잘 잤고 컨디션이 나름 괜찮습니다! 힘을 더 쓸 수 있을지도요...? (신속 II 3분, 힘 II 30초)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 180, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 1, true, false))
                }
                1 -> {
                    p.sendMessage(text("머리가 매우 멍하고 컨디션이 좋지 않습니다... (구속 2분)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 120, 0, true, false))
                }
                2 -> {
                    p.sendMessage(text("악몽을 꾸었습니다. 다시는 생각해보기도 싫은 매우 끔찍한 악몽이었습니다. (구속 II 3분, 채굴피로 1분)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 180, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 60, 0, true, false))
                }
                3 -> {
                    p.sendMessage(text("컨디션이 평범합니다. 평소처럼 활동 할 수 있을듯 합니다. (신속 1분 30초)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 90, 0, true, false))
                }
                4 -> {
                    p.sendMessage(text("수면마비(가위눌림)을 겪으셨습니다. 악몽과 비슷하게 썩 좋지많은 않았습니다. (구속 II 1분, 채굴피로 40초)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 60, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 40, 0, true, false))
                }
                5 -> {
                    p.sendMessage(text("자는 자세가 뭔가 잘못된걸까요...? 침대에서 굴러떨어지셨습니다.. (멀미 1분)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 20 * 60, 0, true, false))
                }
                6 -> {
                    p.sendMessage(text("간만에 매우 편하게 잠을 잤습니다! 고된 노동도 가볍게 느껴질거같은데요? (신속 II 3분, 힘 II 15초, 성급함 2분)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 180, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 15, 1, true, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 120, 0, true, false))
                }
            }
        }
    }

    // Milk Event, Decrease Thirst
    @EventHandler
    fun onPlayerItemConsume(e: PlayerItemConsumeEvent) {
        val p = e.player
        val type = e.item.type

        if (type == Material.MILK_BUCKET) {
            when (nextInt(3)) {
                0 -> {
                    p.sendMessage(text("괜찮은 우유를 드신 것 같습니다. (기본 우유 효과)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                }
                1 -> {
                    p.sendMessage(text("우유가 상한 건지, 소에 병이 들은건지, 무엇인지는 몰라도 일단 좋은 우유는 아닌것 같습니다... (독 10초)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                    p.addPotionEffect(PotionEffect(PotionEffectType.POISON, 200, 0, true, false))
                }
                2 -> {
                    p.sendMessage(text("오늘따라 우유를 먹을 컨디션은 아닌 것 같네요... (멀미 15초)"))
                    p.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 20 * 15, 0, true, false))
                }
            }
            decreaseThirst(p, DecreaseReason.MILK)
        }
        if (e.item.type == Material.POTION) {
            decreaseThirst(p, DecreaseReason.POTION)
        }
    }

    // Check ending conditions
    @EventHandler
    fun onPlayerAdvancementDone(e: PlayerAdvancementDoneEvent) {
        val firework = FireworkEffect.builder().with(FireworkEffect.Type.STAR).withColor(org.bukkit.Color.AQUA).build()
        val advc = e.advancement

        if (advc.key.toString() == "minecraft:end/kill_dragon") {
            getInstance().config.set("kill-dragon", true)
            getInstance().saveConfig()
        }
        if (advc.key.toString() == "minecraft:nether/create_full_beacon") {
            if (getInstance().config.getBoolean("kill-dragon")) {
                server.onlinePlayers.forEach {
                    val loc = it.location.add(0.0, 0.9, 0.0)

                    loc.world.playFirework(loc, firework)
                }
                server.scheduler.cancelTasks(getInstance())
                ending = true
            }
        }
    }

    @EventHandler
    fun onPaperServerListPing(e: PaperServerListPingEvent) {

        // The most fucking motd ever seen in your life lmfao

        val motdString = ("X X X X X X X V V I V V I V V I V V I L L L L L .")
        val localDateTime = LocalDateTime.now()
        val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")

        val words = motdString.split(" ").toMutableList()

        for (i in words.indices) {
            words[i] = "${ChatColor.of(Color(nextInt(0xFF0000)))}" + words[i]
        }

        val stringJoiner = StringJoiner("")

        for (word in words) {
            stringJoiner.add(word)
        }

        val sentence = stringJoiner.toString()

        // Project start date; it has been planned earlier, but I forgot to set up the Wakatime & in this date I actually started writing in-game managing codes.

        e.numPlayers = 20211122
        e.maxPlayers = localDateTime.format(dateFormat).toInt()

        e.playerSample.clear()

        e.motd(text(sentence))
    }
}