package me.haydencheers.prep.scripts.simplag

@Record data class InjectedAssignmentRecord(
    var source: String,
    var destination: String
)

@Record data class InjectedFileRecord(
    var source: String,
    var injectedInto: String,
    var variantId: Int,
    var destination: String
)

@Record data class InjectedClassRecord(
    var projectName: String,
    var variantId: Int,

    var seedName: String,
    var seedFile: String,
    var seedClassName: String,

    var injectionTarget: String
)

@Record data class InjectedMethodRecord(
    var projectName: String,
    var variantId: Int,

    var seedName: String,
    var seedFile: String,
    var seedClassName: String,
    var seedMethod: String,

    var injectionTarget: String,
    var injectionClass: String
)

@Record data class InjectedStatementRecord(
    var projectName: String,
    var variantId: Int,

    var seedName: String,
    var seedFile: String,
    var seedClassName: String,
    var seedMethod: String,
    var seedStatement: String,

    var injectionTarget: String,
    var injectionClass: String,
    var injectedMethodName: String
)