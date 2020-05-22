package me.haydencheers.prep.scripts.simplag

import javax.inject.Singleton

@Singleton
class Analytics {
    var injectedWholeAssignment = mutableListOf<InjectedAssignmentRecord>()
    var injectedFiles = mutableListOf<InjectedFileRecord>()
    var injectedClasses = mutableListOf<InjectedClassRecord>()
    var injectedMethods = mutableListOf<InjectedMethodRecord>()
    var injectedStatements = mutableListOf<InjectedStatementRecord>()

    var mutateAddComment = mutableListOf<AddCommentRecord>()
    var mutateRemoveComment = mutableListOf<RemoveCommentRecord>()
    var mutateModifyComment = mutableListOf<MutateCommentRecord>()
    var mutateRenameIdentifiers = mutableListOf<RenameIdentifiersRecord>()
    var mutateReorderStatements = mutableListOf<ReorderStatementsRecord>()
    var mutateReorderMembers = mutableListOf<ReorderMembersRecord>()
    var mutateSwapOperands = mutableListOf<SwapOperandsRecord>()
    var mutateUpcastDataType = mutableListOf<UpcastDataTypeRecord>()
    var mutateSwapForToWhile = mutableListOf<SwapForToWhileRecord>()
    var mutateExpandCompoundAssignment = mutableListOf<ExpandCompoundAssignmentRecord>()
    var mutateExpandIncrementDecrement = mutableListOf<ExpandIncrementDecrementRecord>()
    var mutateSplitVariableDeclaration = mutableListOf<SplitVariableDeclarationsRecord>()
    var mutateAssignDefaultValue = mutableListOf<AssignDefaultValueRecord>()
    var mutateSplitDeclarationAndAssignment = mutableListOf<SplitDeclarationAndAssignmentRecord>()
}

