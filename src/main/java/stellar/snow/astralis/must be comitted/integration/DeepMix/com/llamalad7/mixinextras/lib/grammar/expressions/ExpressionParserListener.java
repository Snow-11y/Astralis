package com.llamalad7.mixinextras.lib.grammar.expressions;

import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ParseTreeListener;
import com.llamalad7.mixinextras.lib.grammar.expressions.ExpressionParser;

public interface ExpressionParserListener
extends ParseTreeListener {
    public void enterRoot(ExpressionParser.RootContext var1);

    public void exitRoot(ExpressionParser.RootContext var1);

    public void enterMemberAssignmentStatement(ExpressionParser.MemberAssignmentStatementContext var1);

    public void exitMemberAssignmentStatement(ExpressionParser.MemberAssignmentStatementContext var1);

    public void enterArrayStoreStatement(ExpressionParser.ArrayStoreStatementContext var1);

    public void exitArrayStoreStatement(ExpressionParser.ArrayStoreStatementContext var1);

    public void enterIdentifierAssignmentStatement(ExpressionParser.IdentifierAssignmentStatementContext var1);

    public void exitIdentifierAssignmentStatement(ExpressionParser.IdentifierAssignmentStatementContext var1);

    public void enterReturnStatement(ExpressionParser.ReturnStatementContext var1);

    public void exitReturnStatement(ExpressionParser.ReturnStatementContext var1);

    public void enterThrowStatement(ExpressionParser.ThrowStatementContext var1);

    public void exitThrowStatement(ExpressionParser.ThrowStatementContext var1);

    public void enterExpressionStatement(ExpressionParser.ExpressionStatementContext var1);

    public void exitExpressionStatement(ExpressionParser.ExpressionStatementContext var1);

    public void enterBitwiseXorExpression(ExpressionParser.BitwiseXorExpressionContext var1);

    public void exitBitwiseXorExpression(ExpressionParser.BitwiseXorExpressionContext var1);

    public void enterClassConstantExpression(ExpressionParser.ClassConstantExpressionContext var1);

    public void exitClassConstantExpression(ExpressionParser.ClassConstantExpressionContext var1);

    public void enterStaticMethodCallExpression(ExpressionParser.StaticMethodCallExpressionContext var1);

    public void exitStaticMethodCallExpression(ExpressionParser.StaticMethodCallExpressionContext var1);

    public void enterBoolLitExpression(ExpressionParser.BoolLitExpressionContext var1);

    public void exitBoolLitExpression(ExpressionParser.BoolLitExpressionContext var1);

    public void enterUnaryExpression(ExpressionParser.UnaryExpressionContext var1);

    public void exitUnaryExpression(ExpressionParser.UnaryExpressionContext var1);

    public void enterFreeMethodReferenceExpression(ExpressionParser.FreeMethodReferenceExpressionContext var1);

    public void exitFreeMethodReferenceExpression(ExpressionParser.FreeMethodReferenceExpressionContext var1);

    public void enterConstructorReferenceExpression(ExpressionParser.ConstructorReferenceExpressionContext var1);

    public void exitConstructorReferenceExpression(ExpressionParser.ConstructorReferenceExpressionContext var1);

    public void enterInstantiationExpression(ExpressionParser.InstantiationExpressionContext var1);

    public void exitInstantiationExpression(ExpressionParser.InstantiationExpressionContext var1);

    public void enterIntLitExpression(ExpressionParser.IntLitExpressionContext var1);

    public void exitIntLitExpression(ExpressionParser.IntLitExpressionContext var1);

    public void enterThisExpression(ExpressionParser.ThisExpressionContext var1);

    public void exitThisExpression(ExpressionParser.ThisExpressionContext var1);

    public void enterDecimalLitExpression(ExpressionParser.DecimalLitExpressionContext var1);

    public void exitDecimalLitExpression(ExpressionParser.DecimalLitExpressionContext var1);

    public void enterMethodCallExpression(ExpressionParser.MethodCallExpressionContext var1);

    public void exitMethodCallExpression(ExpressionParser.MethodCallExpressionContext var1);

    public void enterInstanceofExpression(ExpressionParser.InstanceofExpressionContext var1);

    public void exitInstanceofExpression(ExpressionParser.InstanceofExpressionContext var1);

    public void enterWildcardExpression(ExpressionParser.WildcardExpressionContext var1);

    public void exitWildcardExpression(ExpressionParser.WildcardExpressionContext var1);

    public void enterArrayLitExpression(ExpressionParser.ArrayLitExpressionContext var1);

    public void exitArrayLitExpression(ExpressionParser.ArrayLitExpressionContext var1);

    public void enterStringLitExpression(ExpressionParser.StringLitExpressionContext var1);

    public void exitStringLitExpression(ExpressionParser.StringLitExpressionContext var1);

    public void enterEqualityExpression(ExpressionParser.EqualityExpressionContext var1);

    public void exitEqualityExpression(ExpressionParser.EqualityExpressionContext var1);

    public void enterMultiplicativeExpression(ExpressionParser.MultiplicativeExpressionContext var1);

    public void exitMultiplicativeExpression(ExpressionParser.MultiplicativeExpressionContext var1);

    public void enterBitwiseOrExpression(ExpressionParser.BitwiseOrExpressionContext var1);

    public void exitBitwiseOrExpression(ExpressionParser.BitwiseOrExpressionContext var1);

    public void enterParenthesizedExpression(ExpressionParser.ParenthesizedExpressionContext var1);

    public void exitParenthesizedExpression(ExpressionParser.ParenthesizedExpressionContext var1);

    public void enterAdditiveExpression(ExpressionParser.AdditiveExpressionContext var1);

    public void exitAdditiveExpression(ExpressionParser.AdditiveExpressionContext var1);

    public void enterMemberAccessExpression(ExpressionParser.MemberAccessExpressionContext var1);

    public void exitMemberAccessExpression(ExpressionParser.MemberAccessExpressionContext var1);

    public void enterBoundMethodReferenceExpression(ExpressionParser.BoundMethodReferenceExpressionContext var1);

    public void exitBoundMethodReferenceExpression(ExpressionParser.BoundMethodReferenceExpressionContext var1);

    public void enterShiftExpression(ExpressionParser.ShiftExpressionContext var1);

    public void exitShiftExpression(ExpressionParser.ShiftExpressionContext var1);

    public void enterCapturingExpression(ExpressionParser.CapturingExpressionContext var1);

    public void exitCapturingExpression(ExpressionParser.CapturingExpressionContext var1);

    public void enterNullExpression(ExpressionParser.NullExpressionContext var1);

    public void exitNullExpression(ExpressionParser.NullExpressionContext var1);

    public void enterIdentifierExpression(ExpressionParser.IdentifierExpressionContext var1);

    public void exitIdentifierExpression(ExpressionParser.IdentifierExpressionContext var1);

    public void enterBitwiseAndExpression(ExpressionParser.BitwiseAndExpressionContext var1);

    public void exitBitwiseAndExpression(ExpressionParser.BitwiseAndExpressionContext var1);

    public void enterComparisonExpression(ExpressionParser.ComparisonExpressionContext var1);

    public void exitComparisonExpression(ExpressionParser.ComparisonExpressionContext var1);

    public void enterSuperCallExpression(ExpressionParser.SuperCallExpressionContext var1);

    public void exitSuperCallExpression(ExpressionParser.SuperCallExpressionContext var1);

    public void enterCastExpression(ExpressionParser.CastExpressionContext var1);

    public void exitCastExpression(ExpressionParser.CastExpressionContext var1);

    public void enterNewArrayExpression(ExpressionParser.NewArrayExpressionContext var1);

    public void exitNewArrayExpression(ExpressionParser.NewArrayExpressionContext var1);

    public void enterArrayAccessExpression(ExpressionParser.ArrayAccessExpressionContext var1);

    public void exitArrayAccessExpression(ExpressionParser.ArrayAccessExpressionContext var1);

    public void enterIdentifierName(ExpressionParser.IdentifierNameContext var1);

    public void exitIdentifierName(ExpressionParser.IdentifierNameContext var1);

    public void enterWildcardName(ExpressionParser.WildcardNameContext var1);

    public void exitWildcardName(ExpressionParser.WildcardNameContext var1);

    public void enterNameWithDims(ExpressionParser.NameWithDimsContext var1);

    public void exitNameWithDims(ExpressionParser.NameWithDimsContext var1);

    public void enterArguments(ExpressionParser.ArgumentsContext var1);

    public void exitArguments(ExpressionParser.ArgumentsContext var1);

    public void enterNonEmptyArguments(ExpressionParser.NonEmptyArgumentsContext var1);

    public void exitNonEmptyArguments(ExpressionParser.NonEmptyArgumentsContext var1);
}

