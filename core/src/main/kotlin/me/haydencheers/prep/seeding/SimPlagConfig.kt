package me.haydencheers.prep.seeding

import java.time.Instant
import javax.json.bind.annotation.JsonbTransient
import kotlin.random.Random

class SimPlagConfig {
    lateinit var input: String
    lateinit var injectionSources: String
    lateinit var output: String

    var randomSeed: Long = Instant.now().epochSecond

    var inject: InjectConfig = InjectConfig()
    var mutate: MutateConfig = MutateConfig()

    var copies: Int = 10

    @JsonbTransient
    lateinit var random: Random
}

class InjectConfig {
    var injectAssignment: Boolean = true
    var injectAssignmentCount: Int = 5

    var injectFile: Boolean = true
    var injectFileChance: Double = 1.0

    var injectClass: Boolean = true
    var injectClassChance: Double = 1.0
    var injectClassMaxCount: Int = 1

    var injectMethod: Boolean = true
    var injectMethodChance: Double = 1.0
    var injectMethodMaxCount = 2

    var injectBlock: Boolean = true
    var injectBlockChance: Double = 1.0
    var injectBlockMaxStatements: Int = 10
}

class MutateConfig {
    var commenting = CommentingConfig()

//    var changeWhitespace = true
//    var changeWhitespaceChance = 0.1
//    var changeFormatting = true
//    var changeFormattingChance = 0.1

    var renameIdentifiers = true
    var renameIdentifiersChance = 0.1

    var reorderStatements = true
    var reorderStatementsChance = 0.1
    var reorderMemberDeclarations = true
    var reorderMemberDeclarationsChance = 0.1

    var swapOperands = true
    var swapOperandsChance = 0.1

    var upcastDataTypes = true
    var upcastDataTypesChance = 0.1

    var switchToIf = true
    var switchToIfChance = 0.1
    var forToWhile = true
    var forToWhileChance = 0.1
    var expandCompoundAssignment = true
    var expandCompoundAssignmentChance = 0.1
    var expandIncDec = true
    var expandIncDecChance= 0.1

    var splitVariableDeclarations = true
    var splitVariableDeclarationsChance = 0.1
    var assignDefaultValue = true
    var assignDefaultValueChance = 0.1
    var splitDeclAndAssignment = true
    var splitDeclAndAssignmentChance = 0.1
    var groupVariableDeclarations = true
    var groupVariableDeclarationsChance = 0.1
}

class CommentingConfig {
    var add = true
    var addChance = 0.6

    var remove = true
    var removeChance = 0.4

    var mutate = true
    var mutateChance = 1.0
}