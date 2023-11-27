package com.scarlet.coffeeshop

import com.scarlet.coffeeshop.model.CoffeeBean
import com.scarlet.coffeeshop.model.Espresso
import com.scarlet.coffeeshop.model.Milk
import com.scarlet.coffeeshop.util.log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select

// actor 로 Request를 전달하고, 동작 완료 후 CompletedDeferred를 통해서 결과를 전달한다.
// EspressoMachine1에서는 불필요한 Channel을 계속 생성 하였으나, 여기서는 CompletedDeferred를 사용한다.
@ObsoleteCoroutinesApi
class EspressoMachine2(scope: CoroutineScope) : CoroutineScope by scope {

    data class EspressoRequest(
        val groundBeans: CoffeeBean.GroundBeans,
        val espressoDeferred: CompletableDeferred<Espresso>
    )

    private val portaFilter1 = actor<EspressoRequest>(CoroutineName("portaFilter-1")) {
        channel.consumeEach {
            val espresso = processEspresso(it.groundBeans)
            it.espressoDeferred.complete(espresso)
        }
    }

    private val portaFilter2 = actor<EspressoRequest>(CoroutineName("portaFilter-2")) {
        channel.consumeEach {
            val espresso = processEspresso(it.groundBeans)
            it.espressoDeferred.complete(espresso)
        }
    }

    suspend fun pullEspressoShot(groundBeans: CoffeeBean.GroundBeans): Espresso {
        val espressoDeferred = CompletableDeferred<Espresso>()
        val espressoRequest = EspressoRequest(groundBeans, espressoDeferred)
        return select {
          portaFilter1.onSend(espressoRequest) {espressoRequest.espressoDeferred.await()}
          portaFilter2.onSend(espressoRequest) {espressoRequest.espressoDeferred.await()}
        }
    }

    private suspend fun processEspresso(groundBeans: CoffeeBean.GroundBeans): Espresso {
        log("pulling espresso shot")
        delay(600)
        return Espresso(groundBeans)
    }

    data class SteamedMilkRequest(
        val milk: Milk,
        val steamMilkDeferred: CompletableDeferred<Milk.SteamedMilk>
    )

    private val steamWand1 = actor<SteamedMilkRequest>(CoroutineName("streamWand-1")) {
        channel.consumeEach {
            val steamedMilk = processSteamMilk(it.milk)
            it.steamMilkDeferred.complete(steamedMilk)
        }
    }

    private val steamWand2 = actor<SteamedMilkRequest>(CoroutineName("streamWand-2")) {
        channel.consumeEach {
            val steamedMilk = processSteamMilk(it.milk)
            it.steamMilkDeferred.complete(steamedMilk)
        }
    }

    suspend fun steamMilk(milk: Milk): Milk.SteamedMilk {
        val steamMilkDeferred = CompletableDeferred<Milk.SteamedMilk>()
        val steamedRequest = SteamedMilkRequest(milk, steamMilkDeferred)
        return select {
            steamWand1.onSend(steamedRequest) {steamedRequest.steamMilkDeferred.await()}
            steamWand2.onSend(steamedRequest) {steamedRequest.steamMilkDeferred.await()}
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