package me.haydencheers.prep.normalise

import java.io.File


object Forker {
    fun exec(
        cls: Class<*>,
        args: Array<String> = emptyArray(),
        props: Array<String> = emptyArray(),
        env: Map<String, String> = emptyMap()
    ): Int {
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome + File.separator + "bin" + File.separator + "java"
        val classpath = System.getProperty("java.class.path")
        val className = cls.name

        val command = mutableListOf<String>()
        command.add(javaBin)
        command.addAll(props)
        command.add("-cp")
        command.add(classpath)
        command.add(className)
        command.addAll(args)

        val builder = ProcessBuilder(command)
        env.toMap(builder.environment())
        val process = builder
            .inheritIO()
            .start()
        process.waitFor()

        return process.exitValue()
    }
}