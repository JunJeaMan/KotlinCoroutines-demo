package com.scarlet.coffeeshop

import com.scarlet.coffeeshop.model.CoffeeBean
import com.scarlet.coffeeshop.model.Espresso
import com.scarlet.coffeeshop.model.Milk
import com.scarlet.coffeeshop.util.log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select

// actor 로 Request를 전달하고, 동작 완료 후 Request 내부에 있는 channel 로 결과를 전달한다.
@ObsoleteCoroutinesApi
class EspressoMachine1(scope: CoroutineScope) : CoroutineScope by scope {

    data class EspressoRequest(
        val groundBeans: CoffeeBean.GroundBeans,
        val espressoChannel: Channel<Espresso>
    )

    private val portaFilter1 = actor<EspressoRequest>(CoroutineName("portaFilter-1")) {
        channel.consumeEach {
            val espresso = processEspresso(it.groundBeans)
            it.espressoChannel.send(espresso)
        }
    }

    private val portaFilter2 = actor<EspressoRequest>(CoroutineName("portaFilter-2")) {
        channel.consumeEach {
            val espresso = processEspresso(it.groundBeans)
            it.espressoChannel.send(espresso)
        }
    }

    suspend fun pullEspressoShot(groundBeans: CoffeeBean.GroundBeans): Espresso {
//        1개의 portaFilter 가 있을때
//        val espressoChannel = Channel<Espresso>()
//        val espressoRequest = EspressoRequest(groundBeans, espressoChannel)
//        portaFilter1.send(espressoRequest)
//        return espressoChannel.receive()

//        2개 이상의 portaFilter 가 있을때
        val espressoChannel = Channel<Espresso>()
        val espressoRequest = EspressoRequest(groundBeans, espressoChannel)
        return select {
          portaFilter1.onSend(espressoRequest) {espressoRequest.espressoChannel.receive()}
          portaFilter2.onSend(espressoRequest) {espressoRequest.espressoChannel.receive()}
        }
    }

    private suspend fun processEspresso(groundBeans: CoffeeBean.GroundBeans): Espresso {
        log("pulling espresso shot")
        delay(600)
        return Espresso(groundBeans)
    }

    data class SteamedMilkRequest(
        val milk: Milk,
        val steamMilkChannel: Channel<Milk.SteamedMilk>
    )

    private val steamWand1 = actor<SteamedMilkRequest>(CoroutineName("streamWand-1")) {
        channel.consumeEach {
            val steamedMilk = processSteamMilk(it.milk)
            it.steamMilkChannel.send(steamedMilk)
        }
    }

    private val steamWand2 = actor<SteamedMilkRequest>(CoroutineName("streamWand-2")) {
        channel.consumeEach {
            val steamedMilk = processSteamMilk(it.milk)
            it.steamMilkChannel.send(steamedMilk)
        }
    }

    suspend fun steamMilk(milk: Milk): Milk.SteamedMilk {
        val steamedMilkChannel = Channel<Milk.SteamedMilk>()
        val steamedRequest = SteamedMilkRequest(milk, steamedMilkChannel)
        return select {
            steamWand1.onSend(steamedRequest) {steamedRequest.steamMilkChannel.receive()}
            steamWand2.onSend(steamedRequest) {steamedRequest.steamMilkChannel.receive()}
        }

    }

    private suspend fun processSteamMilk(milk: Milk): Milk.SteamedMilk {
        log("steaming milk")
        delay(300)
        return Milk.SteamedMilk(milk)
    }


    fun shutDown() {
        portaFilter1.close()
        portaFilter2.close()
        steamWand1.close()
        steamWand2.close()
    }
}