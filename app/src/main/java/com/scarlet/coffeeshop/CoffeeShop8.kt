@file:OptIn(ObsoleteCoroutinesApi::class)

package com.scarlet.coffeeshop

import com.scarlet.coffeeshop.model.*
import com.scarlet.coffeeshop.util.log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

// 1~5번까지는 Espresso 머진의 한계가 없었으나, 여기서는 Espresso machine 에 한정된 갯수의
// porta filter 와 steam wand 가 존재한다는 가정으로 진행한다.
// produce가 아니라 Flow를 이용해서 구현한다.

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
fun main() = runBlocking {
    val orders = listOf(
        Menu.Cappuccino(CoffeeBean.Regular, Milk.Whole),
        Menu.Cappuccino(CoffeeBean.Premium, Milk.Breve),
        Menu.Cappuccino(CoffeeBean.Regular, Milk.NonFat),
        Menu.Cappuccino(CoffeeBean.Decaf, Milk.Whole),
        Menu.Cappuccino(CoffeeBean.Regular, Milk.NonFat),
        Menu.Cappuccino(CoffeeBean.Decaf, Milk.NonFat)
    ).onEach { log(it) }

    val orderFlow = orders.asFlow()
        .produceIn(this + CoroutineName("cashier"))
        .receiveAsFlow()


    val espressoMachine1 = EspressoMachine2(this)

    val time = measureTimeMillis {
        flowOf(
            processOrders(espressoMachine1, orderFlow),
            processOrders(espressoMachine1, orderFlow)
        ).flattenMerge(2)
            .collect { log("serve : $it") }
    }
    log("time: $time ms")

    espressoMachine1.shutDown()
}

private suspend fun processOrders(
    espressoMachine: EspressoMachine2,
    orderFlow: Flow<Menu.Cappuccino>
): Flow<Beverage.Cappuccino> {
    return orderFlow.map {
        log("Processing order: $it")
        val groundBeans = grindCoffeeBeans(it.beans)
        val espresso = espressoMachine.pullEspressoShot(groundBeans)
        val steamedMilk = espressoMachine.steamMilk(it.milk)
        makeCappuccino(it, espresso, steamedMilk)
    }
}

private suspend fun grindCoffeeBeans(beans: CoffeeBean): CoffeeBean.GroundBeans {
    log("grinding coffee beans")
    delay(1000)
    return CoffeeBean.GroundBeans(beans)
}

private suspend fun makeCappuccino(
    order: Menu.Cappuccino,
    espresso: Espresso,
    steamedMilk: Milk.SteamedMilk
): Beverage.Cappuccino {
    log("making cappuccino")
    delay(100)
    return Beverage.Cappuccino(order, espresso, steamedMilk)
}
