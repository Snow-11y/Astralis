package com.llamalad7.mixinextras.expression.impl;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ArrayAccessExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ArrayLiteralExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ArrayStoreExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.BinaryExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.BooleanLiteralExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.BoundMethodReferenceExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.CapturingExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.CastExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ClassConstantExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ComparisonExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ConstructorReferenceExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.DecimalLiteralExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.FreeMethodReferenceExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.IdentifierAssignmentExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.IdentifierExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.InstanceofExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.InstantiationExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.IntLiteralExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.MemberAccessExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.MemberAssignmentExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.MethodCallExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.NewArrayExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.NullLiteralExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ReturnExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.StaticMethodCallExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.StringLiteralExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SuperCallExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ThisExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.ThrowExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.UnaryExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.WildcardExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.ArrayTypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.DefinedMemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.DefinedTypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.WildcardIdentifier;
import com.llamalad7.mixinextras.lib.antlr.runtime.ANTLRErrorListener;
import com.llamalad7.mixinextras.lib.antlr.runtime.CharStreams;
import com.llamalad7.mixinextras.lib.antlr.runtime.CommonTokenStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Parser;
import com.llamalad7.mixinextras.lib.antlr.runtime.ParserRuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.RecognitionException;
import com.llamalad7.mixinextras.lib.antlr.runtime.Recognizer;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNConfigSet;
import com.llamalad7.mixinextras.lib.antlr.runtime.dfa.DFA;
import com.llamalad7.mixinextras.lib.grammar.expressions.ExpressionLexer;
import com.llamalad7.mixinextras.lib.grammar.expressions.ExpressionParser;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionParserFacade {
    private final String expression;
    private boolean hasExplicitCapture = false;

    public ExpressionParserFacade(String expression) {
        this.expression = expression;
    }

    public static Expression parse(String input) {
        ExpressionLexer lexer = new ExpressionLexer(CharStreams.fromString(input));
        ExpressionParserFacade.setupErrorListeners(lexer, input);
        ExpressionParser parser = new ExpressionParser(new CommonTokenStream(lexer));
        ExpressionParserFacade.setupErrorListeners(parser, input);
        ExpressionParserFacade facade = new ExpressionParserFacade(input);
        Expression parsed = facade.parse(parser.root().statement());
        if (facade.hasExplicitCapture) {
            return parsed;
        }
        return new CapturingExpression(new ExpressionSource(input, 0, input.length() - 1), parsed);
    }

    private Expression parse(ExpressionParser.StatementContext statement) {
        if (statement instanceof ExpressionParser.MemberAssignmentStatementContext) {
            return this.parse((ExpressionParser.MemberAssignmentStatementContext)statement);
        }
        if (statement instanceof ExpressionParser.ArrayStoreStatementContext) {
            return this.parse((ExpressionParser.ArrayStoreStatementContext)statement);
        }
        if (statement instanceof ExpressionParser.IdentifierAssignmentStatementContext) {
            return this.parse((ExpressionParser.IdentifierAssignmentStatementContext)statement);
        }
        if (statement instanceof ExpressionParser.ReturnStatementContext) {
            return this.parse((ExpressionParser.ReturnStatementContext)statement);
        }
        if (statement instanceof ExpressionParser.ThrowStatementContext) {
            return this.parse((ExpressionParser.ThrowStatementContext)statement);
        }
        if (statement instanceof ExpressionParser.ExpressionStatementContext) {
            return this.parse((ExpressionParser.ExpressionStatementContext)statement);
        }
        throw this.unimplemented();
    }

    private MemberAssignmentExpression parse(ExpressionParser.MemberAssignmentStatementContext statement) {
        return new MemberAssignmentExpression(this.getSource(statement), this.parse(statement.receiver), this.parseMemberId(statement.memberName), this.parse(statement.value));
    }

    private ArrayStoreExpression parse(ExpressionParser.ArrayStoreStatementContext statement) {
        return new ArrayStoreExpression(this.getSource(statement), this.parse(statement.arr), this.parse(statement.index), this.parse(statement.value));
    }

    private IdentifierAssignmentExpression parse(ExpressionParser.IdentifierAssignmentStatementContext statement) {
        return new IdentifierAssignmentExpression(this.getSource(statement), this.parseMemberId(statement.identifier), this.parse(statement.value));
    }

    private ReturnExpression parse(ExpressionParser.ReturnStatementContext statement) {
        return new ReturnExpression(this.getSource(statement), this.parse(statement.value));
    }

    private ThrowExpression parse(ExpressionParser.ThrowStatementContext statement) {
        return new ThrowExpression(this.getSource(statement), this.parse(statement.value));
    }

    private Expression parse(ExpressionParser.ExpressionStatementContext statement) {
        return this.parse(statement.expression());
    }

    private Expression parse(ExpressionParser.ExpressionContext expression) {
        if (expression instanceof ExpressionParser.CapturingExpressionContext) {
            return this.parse((ExpressionParser.CapturingExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ParenthesizedExpressionContext) {
            return this.parse((ExpressionParser.ParenthesizedExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.SuperCallExpressionContext) {
            return this.parse((ExpressionParser.SuperCallExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.MethodCallExpressionContext) {
            return this.parse((ExpressionParser.MethodCallExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.StaticMethodCallExpressionContext) {
            return this.parse((ExpressionParser.StaticMethodCallExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.BoundMethodReferenceExpressionContext) {
            return this.parse((ExpressionParser.BoundMethodReferenceExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.FreeMethodReferenceExpressionContext) {
            return this.parse((ExpressionParser.FreeMethodReferenceExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ConstructorReferenceExpressionContext) {
            return this.parse((ExpressionParser.ConstructorReferenceExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ArrayAccessExpressionContext) {
            return this.parse((ExpressionParser.ArrayAccessExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ClassConstantExpressionContext) {
            return this.parse((ExpressionParser.ClassConstantExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.MemberAccessExpressionContext) {
            return this.parse((ExpressionParser.MemberAccessExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.NewArrayExpressionContext) {
            return this.parse((ExpressionParser.NewArrayExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ArrayLitExpressionContext) {
            return this.parse((ExpressionParser.ArrayLitExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.UnaryExpressionContext) {
            return this.parse((ExpressionParser.UnaryExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.CastExpressionContext) {
            return this.parse((ExpressionParser.CastExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.InstantiationExpressionContext) {
            return this.parse((ExpressionParser.InstantiationExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.MultiplicativeExpressionContext) {
            return this.parse((ExpressionParser.MultiplicativeExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.AdditiveExpressionContext) {
            return this.parse((ExpressionParser.AdditiveExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ShiftExpressionContext) {
            return this.parse((ExpressionParser.ShiftExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ComparisonExpressionContext) {
            return this.parse((ExpressionParser.ComparisonExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.InstanceofExpressionContext) {
            return this.parse((ExpressionParser.InstanceofExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.EqualityExpressionContext) {
            return this.parse((ExpressionParser.EqualityExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.BitwiseAndExpressionContext) {
            return this.parse((ExpressionParser.BitwiseAndExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.BitwiseXorExpressionContext) {
            return this.parse((ExpressionParser.BitwiseXorExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.BitwiseOrExpressionContext) {
            return this.parse((ExpressionParser.BitwiseOrExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.DecimalLitExpressionContext) {
            return this.parse((ExpressionParser.DecimalLitExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.IntLitExpressionContext) {
            return this.parse((ExpressionParser.IntLitExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.StringLitExpressionContext) {
            return this.parse((ExpressionParser.StringLitExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.BoolLitExpressionContext) {
            return this.parse((ExpressionParser.BoolLitExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.NullExpressionContext) {
            return this.parse((ExpressionParser.NullExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.WildcardExpressionContext) {
            return this.parse((ExpressionParser.WildcardExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.ThisExpressionContext) {
            return this.parse((ExpressionParser.ThisExpressionContext)expression);
        }
        if (expression instanceof ExpressionParser.IdentifierExpressionContext) {
            return this.parse((ExpressionParser.IdentifierExpressionContext)expression);
        }
        throw this.unimplemented();
    }

    private CapturingExpression parse(ExpressionParser.CapturingExpressionContext expression) {
        this.hasExplicitCapture = true;
        return new CapturingExpression(this.getSource(expression), this.parse(expression.expr));
    }

    private Expression parse(ExpressionParser.ParenthesizedExpressionContext expression) {
        return this.parse(expression.expr);
    }

    private SuperCallExpression parse(ExpressionParser.SuperCallExpressionContext expression) {
        return new SuperCallExpression(this.getSource(expression), this.parseMemberId(expression.memberName), this.parse(expression.args));
    }

    private MethodCallExpression parse(ExpressionParser.MethodCallExpressionContext expression) {
        return new MethodCallExpression(this.getSource(expression), this.parse(expression.receiver), this.parseMemberId(expression.memberName), this.parse(expression.args));
    }

    private StaticMethodCallExpression parse(ExpressionParser.StaticMethodCallExpressionContext expression) {
        return new StaticMethodCallExpression(this.getSource(expression), this.parseMemberId(expression.memberName), this.parse(expression.args));
    }

    private BoundMethodReferenceExpression parse(ExpressionParser.BoundMethodReferenceExpressionContext expression) {
        return new BoundMethodReferenceExpression(this.getSource(expression), this.parse(expression.receiver), this.parseMemberId(expression.memberName));
    }

    private FreeMethodReferenceExpression parse(ExpressionParser.FreeMethodReferenceExpressionContext expression) {
        return new FreeMethodReferenceExpression(this.getSource(expression), this.parseMemberId(expression.memberName));
    }

    private ConstructorReferenceExpression parse(ExpressionParser.ConstructorReferenceExpressionContext expression) {
        return new ConstructorReferenceExpression(this.getSource(expression), this.parseTypeId(expression.type));
    }

    private ArrayAccessExpression parse(ExpressionParser.ArrayAccessExpressionContext expression) {
        return new ArrayAccessExpression(this.getSource(expression), this.parse(expression.arr), this.parse(expression.index));
    }

    private ClassConstantExpression parse(ExpressionParser.ClassConstantExpressionContext expression) {
        return new ClassConstantExpression(this.getSource(expression), this.parseTypeId(expression.type));
    }

    private MemberAccessExpression parse(ExpressionParser.MemberAccessExpressionContext expression) {
        return new MemberAccessExpression(this.getSource(expression), this.parse(expression.receiver), this.parseMemberId(expression.memberName));
    }

    private NewArrayExpression parse(ExpressionParser.NewArrayExpressionContext expression) {
        return new NewArrayExpression(this.getSource(expression), this.parseTypeId(expression.innerType), this.parse(expression.dims), expression.blankDims.size());
    }

    private ArrayLiteralExpression parse(ExpressionParser.ArrayLitExpressionContext expression) {
        return new ArrayLiteralExpression(this.getSource(expression), this.parseTypeId(expression.elementType), this.parse(expression.values));
    }

    private UnaryExpression parse(ExpressionParser.UnaryExpressionContext expression) {
        UnaryExpression.Operator op;
        switch (expression.op.getType()) {
            case 19: {
                op = UnaryExpression.Operator.MINUS;
                break;
            }
            case 23: {
                op = UnaryExpression.Operator.BITWISE_NOT;
                break;
            }
            default: {
                throw this.unimplemented();
            }
        }
        return new UnaryExpression(this.getSource(expression), op, this.parse(expression.expr));
    }

    private CastExpression parse(ExpressionParser.CastExpressionContext expression) {
        return new CastExpression(this.getSource(expression), this.parseTypeId(expression.type), this.parse(expression.expr));
    }

    private InstantiationExpression parse(ExpressionParser.InstantiationExpressionContext expression) {
        return new InstantiationExpression(this.getSource(expression), this.parseTypeId(expression.type), this.parse(expression.args));
    }

    private BinaryExpression parse(ExpressionParser.MultiplicativeExpressionContext expression) {
        BinaryExpression.Operator op;
        switch (expression.op.getType()) {
            case 20: {
                op = BinaryExpression.Operator.MULT;
                break;
            }
            case 21: {
                op = BinaryExpression.Operator.DIV;
                break;
            }
            case 22: {
                op = BinaryExpression.Operator.MOD;
                break;
            }
            default: {
                throw this.unimplemented();
            }
        }
        return new BinaryExpression(this.getSource(expression), this.parse(expression.left), op, this.parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.AdditiveExpressionContext expression) {
        BinaryExpression.Operator op;
        switch (expression.op.getType()) {
            case 18: {
                op = BinaryExpression.Operator.PLUS;
                break;
            }
            case 19: {
                op = BinaryExpression.Operator.MINUS;
                break;
            }
            default: {
                throw this.unimplemented();
            }
        }
        return new BinaryExpression(this.getSource(expression), this.parse(expression.left), op, this.parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.ShiftExpressionContext expression) {
        BinaryExpression.Operator op;
        switch (expression.op.getType()) {
            case 33: {
                op = BinaryExpression.Operator.SHL;
                break;
            }
            case 34: {
                op = BinaryExpression.Operator.SHR;
                break;
            }
            case 35: {
                op = BinaryExpression.Operator.USHR;
                break;
            }
            default: {
                throw this.unimplemented();
            }
        }
        return new BinaryExpression(this.getSource(expression), this.parse(expression.left), op, this.parse(expression.right));
    }

    private ComparisonExpression parse(ExpressionParser.ComparisonExpressionContext expression) {
        ComparisonExpression.Operator op;
        switch (expression.op.getType()) {
            case 36: {
                op = ComparisonExpression.Operator.LT;
                break;
            }
            case 37: {
                op = ComparisonExpression.Operator.LE;
                break;
            }
            case 38: {
                op = ComparisonExpression.Operator.GT;
                break;
            }
            case 39: {
                op = ComparisonExpression.Operator.GE;
                break;
            }
            default: {
                throw this.unimplemented();
            }
        }
        return new ComparisonExpression(this.getSource(expression), this.parse(expression.left), op, this.parse(expression.right));
    }

    private InstanceofExpression parse(ExpressionParser.InstanceofExpressionContext expression) {
        return new InstanceofExpression(this.getSource(expression), this.parse(expression.expr), this.parseTypeId(expression.type));
    }

    private ComparisonExpression parse(ExpressionParser.EqualityExpressionContext expression) {
        ComparisonExpression.Operator op;
        switch (expression.op.getType()) {
            case 40: {
                op = ComparisonExpression.Operator.EQ;
                break;
            }
            case 41: {
                op = ComparisonExpression.Operator.NE;
                break;
            }
            default: {
                throw this.unimplemented();
            }
        }
        return new ComparisonExpression(this.getSource(expression), this.parse(expression.left), op, this.parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.BitwiseAndExpressionContext expression) {
        return new BinaryExpression(this.getSource(expression), this.parse(expression.left), BinaryExpression.Operator.BITWISE_AND, this.parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.BitwiseXorExpressionContext expression) {
        return new BinaryExpression(this.getSource(expression), this.parse(expression.left), BinaryExpression.Operator.BITWISE_XOR, this.parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.BitwiseOrExpressionContext expression) {
        return new BinaryExpression(this.getSource(expression), this.parse(expression.left), BinaryExpression.Operator.BITWISE_OR, this.parse(expression.right));
    }

    private DecimalLiteralExpression parse(ExpressionParser.DecimalLitExpressionContext expression) {
        return new DecimalLiteralExpression(this.getSource(expression), Double.parseDouble(expression.getText()));
    }

    private IntLiteralExpression parse(ExpressionParser.IntLitExpressionContext expression) {
        return new IntLiteralExpression(this.getSource(expression), Long.parseLong(expression.getText()));
    }

    private StringLiteralExpression parse(ExpressionParser.StringLitExpressionContext expression) {
        String text = expression.getText();
        return new StringLiteralExpression(this.getSource(expression), text.substring(1, text.length() - 1));
    }

    private BooleanLiteralExpression parse(ExpressionParser.BoolLitExpressionContext expression) {
        return new BooleanLiteralExpression(this.getSource(expression), Boolean.parseBoolean(expression.getText()));
    }

    private NullLiteralExpression parse(ExpressionParser.NullExpressionContext expression) {
        return new NullLiteralExpression(this.getSource(expression));
    }

    private WildcardExpression parse(ExpressionParser.WildcardExpressionContext expression) {
        return new WildcardExpression(this.getSource(expression));
    }

    private ThisExpression parse(ExpressionParser.ThisExpressionContext expression) {
        return new ThisExpression(this.getSource(expression));
    }

    private IdentifierExpression parse(ExpressionParser.IdentifierExpressionContext expression) {
        return new IdentifierExpression(this.getSource(expression), expression.getText());
    }

    private MemberIdentifier parseMemberId(ExpressionParser.NameContext name) {
        if (name instanceof ExpressionParser.IdentifierNameContext) {
            return new DefinedMemberIdentifier(name.getText());
        }
        if (name instanceof ExpressionParser.WildcardNameContext) {
            return new WildcardIdentifier();
        }
        throw this.unimplemented();
    }

    private TypeIdentifier parseTypeId(ExpressionParser.NameContext name) {
        if (name instanceof ExpressionParser.IdentifierNameContext) {
            return new DefinedTypeIdentifier(name.getText());
        }
        if (name instanceof ExpressionParser.WildcardNameContext) {
            return new WildcardIdentifier();
        }
        throw this.unimplemented();
    }

    private TypeIdentifier parseTypeId(ExpressionParser.NameWithDimsContext name) {
        int dims = name.dims.size();
        TypeIdentifier elementType = this.parseTypeId(name.name());
        if (dims == 0) {
            return elementType;
        }
        return new ArrayTypeIdentifier(dims, elementType);
    }

    private List<Expression> parse(ExpressionParser.ArgumentsContext args) {
        return this.parse(args.nonEmptyArguments());
    }

    private List<Expression> parse(ExpressionParser.NonEmptyArgumentsContext args) {
        return args == null ? Collections.emptyList() : this.parse(args.expression());
    }

    private List<Expression> parse(List<ExpressionParser.ExpressionContext> exprs) {
        return exprs.stream().map(this::parse).collect(Collectors.toList());
    }

    private ExpressionSource getSource(ParserRuleContext ctx) {
        return new ExpressionSource(this.expression, ctx.start.getStartIndex(), ctx.stop.getStopIndex());
    }

    private RuntimeException unimplemented() {
        return new IllegalStateException("Unimplemented parser element!");
    }

    private static void setupErrorListeners(Recognizer<?, ?> recognizer, final String expr) {
        recognizer.removeErrorListeners();
        recognizer.addErrorListener(new ANTLRErrorListener(){

            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException(String.format("Failed to parse expression \"%s\": line %s:%s: %s", expr, line, charPositionInLine, msg));
            }

            @Override
            public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
            }

            @Override
            public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
            }

            @Override
            public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
            }
        });
    }
}

