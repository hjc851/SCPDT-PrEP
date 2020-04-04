package me.haydencheers.prep.detection

import me.haydencheers.prep.PrEPPipeline
import me.haydencheers.scpdt.SCPDTool
import me.haydencheers.scpdt.jplag.JPlagSCPDT
import me.haydencheers.scpdt.naive.graph.NaivePDGEditDistanceSCPDT
import me.haydencheers.scpdt.naive.string.NaiveStringEditDistanceSCPDT
import me.haydencheers.scpdt.naive.string.NaiveStringTilingSCPDT
import me.haydencheers.scpdt.naive.token.NaiveTokenEditDistanceSCPDT
import me.haydencheers.scpdt.naive.token.NaiveTokenTilingSCPDT
import me.haydencheers.scpdt.naive.tree.NaiveTreeEditDistanceSCPDT
import me.haydencheers.scpdt.plaggie.PlaggieSCPDT
import me.haydencheers.scpdt.sherlocksydney.SherlockSydneySCPDT
import me.haydencheers.scpdt.sherlockwarwick.SherlockWarwickSCPDT
import me.haydencheers.scpdt.sim.SimWineSCPDTool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolBindingFactory {
    @Inject
    lateinit var prep: PrEPPipeline

    fun produceBindings(): List<SCPDTool> {
        val tools = mutableListOf<SCPDTool>()

        if (prep.config.detection.useJPlag) tools.add(JPlagSCPDT())
        if (prep.config.detection.usePlaggie) tools.add(PlaggieSCPDT())
        if (prep.config.detection.useSim) tools.add(SimWineSCPDTool())
        if (prep.config.detection.useSherlockWarwick) tools.add(SherlockWarwickSCPDT())
        if (prep.config.detection.useSherlockSydney) tools.add(SherlockSydneySCPDT())
        if (prep.config.detection.useNaiveStringEditDistance) tools.add(NaiveStringEditDistanceSCPDT())
        if (prep.config.detection.useNaiveStringTiling) tools.add(NaiveStringTilingSCPDT())
        if (prep.config.detection.useNaiveTokenEditDistance) tools.add(NaiveTokenEditDistanceSCPDT())
        if (prep.config.detection.useNaiveTokenTiling) tools.add(NaiveTokenTilingSCPDT())
        if (prep.config.detection.useNaiveTreeEditDistance) tools.add(NaiveTreeEditDistanceSCPDT())
        if (prep.config.detection.useNaivePDGEditDistance) tools.add(NaivePDGEditDistanceSCPDT())

        return tools
    }
}