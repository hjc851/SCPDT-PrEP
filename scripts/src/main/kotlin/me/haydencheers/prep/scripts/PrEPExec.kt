package me.haydencheers.prep.scripts

import me.haydencheers.prep.Application
import me.haydencheers.prep.normalise.Forker
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object PrEPExec {
    fun execute(config: Path, workingDir: Path, out: Path, err: Path, retryCount: Int): Boolean {
        for (i in 1 .. retryCount) {
            val confp = config.toAbsolutePath().toString()
            val process = Forker.exec(Application::class.java, arrayOf(confp), workingDir = workingDir, out = out, err = err)

            val result = process.waitFor(Config.TIMEOUT, TimeUnit.MINUTES)

            if (result) {
                val exitCode = process.exitValue()
                if (exitCode == 0) return true
            } else {
                System.err.println("Time Limit exceeded")
                process.destroy()
                return false
            }

        }

        return false
    }
}