package me.haydencheers.prep.scripts.simplag

@Record data class AddCommentRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String?
)

@Record data class RemoveCommentRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String?
)

@Record data class MutateCommentRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String?
)

@Record data class RenameIdentifiersRecord (
    var projectName: String,
    var variantId: Int,
    var mappedIdentifiers: Map<String, String>
)

@Record data class ReorderStatementsRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String
)

@Record data class ReorderMembersRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String
)

@Record data class SwapOperandsRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)

@Record data class UpcastDataTypeRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)

@Record data class SwapForToWhileRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)

@Record data class ExpandCompoundAssignmentRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)

@Record data class ExpandIncrementDecrementRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)

@Record data class SplitVariableDeclarationsRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)

@Record data class AssignDefaultValueRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)

@Record data class SplitDeclarationAndAssignmentRecord (
    var projectName: String,
    var variantId: Int,
    var target: String,
    var clazz: String,
    var methodName: String,
    var counter: Int
)