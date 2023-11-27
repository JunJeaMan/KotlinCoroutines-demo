package com.scarlet.coffeeshop

import com.scarlet.coffeeshop.model.Beverage
import com.scarlet.coffeeshop.model.CoffeeBean
import com.scarlet.coffeeshop.model.Espresso
import com.scarlet.coffeeshop.model.Menu
import com.scarlet.coffeeshop.model.Milk
import com.scarlet.coffeeshop.util.log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

// Channel 을 이용해 Menu를 worker (processOrders 쪽으로 전달한다)
// 전달받은 쪽에서는 consumeEach 함수를 통해 하나씩 꺼내서 처리하게 된다.
// 기본 capacity는 RENDEZVOUS 로써 sender, receiver가 둘 다 있어야 동작하기 때문에 동작을 안한다.
// 그래서 BUFFERED로 변경 하였으며, channel을 명시적으로 닫아주지 않으면 종료가 안된다.
fun main() = runBlocking {
    val orders = listOf(
        Menu.Cappuccino(CoffeeBean.Regular, Milk.Whole),
        Menu.Cappuccino(CoffeeBean.Premium, Milk.Breve),
        Menu.Cappuccino(CoffeeBean.Regular, Milk.NonFat),
        Menu.Cappuccino(CoffeeBean.Decaf, Milk.Whole),
        Menu.Cappuccino(CoffeeBean.Regular, Milk.NonFat),
        Menu.Cappuccino(CoffeeBean.Decaf, Milk.NonFat)
    ).onEach { log(it) }

    val channel = Channel<Menu.Cappuccino>(capacity = Channel.BUFFERED)
    orders.forEach {
        channel.send(it)
    }
    channel.close()

    val time = measureTimeMillis {
        coroutineScope {
            launch(CoroutineName("barista-1")) { processOrders(channel) }
            launch(CoroutineName("barista-2")) { processOrders(channel) }
        }
    }
    log("time: $time ms")
}

private suspend fun processOrders(channel: Channel<Menu.Cappuccino>) {
    channel.consumeEach {
        log("Processing order: $it")
        val groundBeans = grindCoffeeBeans(it.beans)
        val espresso = pullEspressoShot(groundBeans)
        val steamedMilk = steamMilk(it.milk)
        val cappuccino = makeCappuccino(it, espresso, steamedMilk)
        log("serve: $cappuccino")
    }
}

private suspend fun grindCoffeeBeans(beans: CoffeeBean): CoffeeBean.GroundBeans {
    log("grinding coffee beans")
    delay(1000)
    return CoffeeBean.GroundBeans(beans)
}

private suspend fun pullEspressoShot(groundBeans: CoffeeBean.GroundBeans): Espresso {
    log("pulling espresso shot")
    delay(600)
    return Espresso(groundBeans)
}

private suspend fun steamMilk(milk: Milk): Milk.SteamedMilk {
    log("steaming milk")
    delay(300)
    return Milk.SteamedMilk(milk)
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
