package me.haydencheers.prep

import javax.enterprise.inject.se.SeContainerInitializer

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        val initialiser = SeContainerInitializer.newInstance()
        initialiser.initialize().use { container ->
            container.select(PrEPPipeline::class.java)
                .get()
                .run(args)
        }
        System.exit(0)
    }
}