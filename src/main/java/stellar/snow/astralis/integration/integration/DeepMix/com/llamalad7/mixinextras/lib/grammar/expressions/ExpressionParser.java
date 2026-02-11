package com.llamalad7.mixinextras.lib.grammar.expressions;

import com.llamalad7.mixinextras.lib.antlr.runtime.FailedPredicateException;
import com.llamalad7.mixinextras.lib.antlr.runtime.NoViableAltException;
import com.llamalad7.mixinextras.lib.antlr.runtime.Parser;
import com.llamalad7.mixinextras.lib.antlr.runtime.ParserRuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.RecognitionException;
import com.llamalad7.mixinextras.lib.antlr.runtime.RuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.RuntimeMetaData;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Vocabulary;
import com.llamalad7.mixinextras.lib.antlr.runtime.VocabularyImpl;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATN;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNDeserializer;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ParserATNSimulator;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PredictionContextCache;
import com.llamalad7.mixinextras.lib.antlr.runtime.dfa.DFA;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ParseTreeListener;
import com.llamalad7.mixinextras.lib.grammar.expressions.ExpressionParserListener;
import java.util.ArrayList;
import java.util.List;

public class ExpressionParser
extends Parser {
    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache;
    public static final String[] ruleNames;
    private static final String[] _LITERAL_NAMES;
    private static final String[] _SYMBOLIC_NAMES;
    public static final Vocabulary VOCABULARY;
    @Deprecated
    public static final String[] tokenNames;
    public static final ATN _ATN;

    private static String[] makeRuleNames() {
        return new String[]{"root", "statement", "expression", "name", "nameWithDims", "arguments", "nonEmptyArguments"};
    }

    private static String[] makeLiteralNames() {
        return new String[]{null, null, null, null, "'?'", "'new'", "'instanceof'", null, "'null'", "'return'", "'throw'", "'this'", "'super'", "'class'", null, null, null, null, "'+'", "'-'", "'*'", "'/'", "'%'", "'~'", "'.'", "','", "'('", "')'", "'['", "']'", "'{'", "'}'", "'@'", "'<<'", "'>>'", "'>>>'", "'<'", "'<='", "'>'", "'>='", "'=='", "'!='", "'&'", "'^'", "'|'", "'='", "'::'", "'++'", "'--'"};
    }

    private static String[] makeSymbolicNames() {
        return new String[]{null, "NewLine", "WS", "StringLit", "Wildcard", "New", "Instanceof", "BoolLit", "NullLit", "Return", "Throw", "This", "Super", "Class", "Reserved", "Identifier", "IntLit", "DecLit", "Plus", "Minus", "Mult", "Div", "Mod", "BitwiseNot", "Dot", "Comma", "LeftParen", "RightParen", "LeftBracket", "RightBracket", "LeftBrace", "RightBrace", "At", "Shl", "Shr", "Ushr", "Lt", "Le", "Gt", "Ge", "Eq", "Ne", "BitwiseAnd", "BitwiseXor", "BitwiseOr", "Assign", "MethodRef", "Increment", "Decrement"};
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override
    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public ExpressionParser(TokenStream input) {
        super(input);
        this._interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public final RootContext root() throws RecognitionException {
        RootContext _localctx = new RootContext(this._ctx, this.getState());
        this.enterRule(_localctx, 0, 0);
        try {
            this.enterOuterAlt(_localctx, 1);
            this.setState(14);
            this.statement();
            this.setState(15);
            this.match(-1);
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            this._errHandler.reportError(this, re);
            this._errHandler.recover(this, re);
        }
        finally {
            this.exitRule();
        }
        return _localctx;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public final StatementContext statement() throws RecognitionException {
        StatementContext _localctx = new StatementContext(this._ctx, this.getState());
        this.enterRule(_localctx, 2, 1);
        try {
            this.setState(39);
            this._errHandler.sync(this);
            switch (((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 0, this._ctx)) {
                case 1: {
                    _localctx = new MemberAssignmentStatementContext(_localctx);
                    this.enterOuterAlt(_localctx, 1);
                    this.setState(17);
                    ((MemberAssignmentStatementContext)_localctx).receiver = this.expression(0);
                    this.setState(18);
                    this.match(24);
                    this.setState(19);
                    ((MemberAssignmentStatementContext)_localctx).memberName = this.name();
                    this.setState(20);
                    this.match(45);
                    this.setState(21);
                    ((MemberAssignmentStatementContext)_localctx).value = this.expression(0);
                    return _localctx;
                }
                case 2: {
                    _localctx = new ArrayStoreStatementContext(_localctx);
                    this.enterOuterAlt(_localctx, 2);
                    this.setState(23);
                    ((ArrayStoreStatementContext)_localctx).arr = this.expression(0);
                    this.setState(24);
                    this.match(28);
                    this.setState(25);
                    ((ArrayStoreStatementContext)_localctx).index = this.expression(0);
                    this.setState(26);
                    this.match(29);
                    this.setState(27);
                    this.match(45);
                    this.setState(28);
                    ((ArrayStoreStatementContext)_localctx).value = this.expression(0);
                    return _localctx;
                }
                case 3: {
                    _localctx = new IdentifierAssignmentStatementContext(_localctx);
                    this.enterOuterAlt(_localctx, 3);
                    this.setState(30);
                    ((IdentifierAssignmentStatementContext)_localctx).identifier = this.name();
                    this.setState(31);
                    this.match(45);
                    this.setState(32);
                    ((IdentifierAssignmentStatementContext)_localctx).value = this.expression(0);
                    return _localctx;
                }
                case 4: {
                    _localctx = new ReturnStatementContext(_localctx);
                    this.enterOuterAlt(_localctx, 4);
                    this.setState(34);
                    this.match(9);
                    this.setState(35);
                    ((ReturnStatementContext)_localctx).value = this.expression(0);
                    return _localctx;
                }
                case 5: {
                    _localctx = new ThrowStatementContext(_localctx);
                    this.enterOuterAlt(_localctx, 5);
                    this.setState(36);
                    this.match(10);
                    this.setState(37);
                    ((ThrowStatementContext)_localctx).value = this.expression(0);
                    return _localctx;
                }
                case 6: {
                    _localctx = new ExpressionStatementContext(_localctx);
                    this.enterOuterAlt(_localctx, 6);
                    this.setState(38);
                    this.expression(0);
                    return _localctx;
                }
            }
            return _localctx;
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            this._errHandler.reportError(this, re);
            this._errHandler.recover(this, re);
            return _localctx;
        }
        finally {
            this.exitRule();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private ExpressionContext expression(int _p) throws RecognitionException {
        ExpressionContext _localctx;
        ParserRuleContext _parentctx = this._ctx;
        int _parentState = this.getState();
        ExpressionContext _prevctx = _localctx = new ExpressionContext(this._ctx, _parentState);
        int _startState = 4;
        this.enterRecursionRule(_localctx, 4, 2, _p);
        try {
            int _alt;
            int _la;
            this.enterOuterAlt(_localctx, 1);
            this.setState(125);
            this._errHandler.sync(this);
            switch (((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 5, this._ctx)) {
                case 1: {
                    _localctx = new CapturingExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(42);
                    this.match(32);
                    this.setState(43);
                    this.match(26);
                    this.setState(44);
                    ((CapturingExpressionContext)_localctx).expr = this.expression(0);
                    this.setState(45);
                    this.match(27);
                    break;
                }
                case 2: {
                    _localctx = new WildcardExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(47);
                    this.match(4);
                    break;
                }
                case 3: {
                    _localctx = new ThisExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(48);
                    this.match(11);
                    break;
                }
                case 4: {
                    _localctx = new IntLitExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(50);
                    this._errHandler.sync(this);
                    _la = this._input.LA(1);
                    if (_la == 19) {
                        this.setState(49);
                        ((IntLitExpressionContext)_localctx).lit = this.match(19);
                    }
                    this.setState(52);
                    this.match(16);
                    break;
                }
                case 5: {
                    _localctx = new DecimalLitExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(54);
                    this._errHandler.sync(this);
                    _la = this._input.LA(1);
                    if (_la == 19) {
                        this.setState(53);
                        ((DecimalLitExpressionContext)_localctx).lit = this.match(19);
                    }
                    this.setState(56);
                    this.match(17);
                    break;
                }
                case 6: {
                    _localctx = new BoolLitExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(57);
                    ((BoolLitExpressionContext)_localctx).lit = this.match(7);
                    break;
                }
                case 7: {
                    _localctx = new NullExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(58);
                    ((NullExpressionContext)_localctx).lit = this.match(8);
                    break;
                }
                case 8: {
                    _localctx = new StringLitExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(59);
                    ((StringLitExpressionContext)_localctx).lit = this.match(3);
                    break;
                }
                case 9: {
                    _localctx = new IdentifierExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(60);
                    ((IdentifierExpressionContext)_localctx).id = this.match(15);
                    break;
                }
                case 10: {
                    _localctx = new ClassConstantExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(61);
                    ((ClassConstantExpressionContext)_localctx).type = this.nameWithDims();
                    this.setState(62);
                    this.match(24);
                    this.setState(63);
                    this.match(13);
                    break;
                }
                case 11: {
                    _localctx = new SuperCallExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(65);
                    this.match(12);
                    this.setState(66);
                    this.match(24);
                    this.setState(67);
                    ((SuperCallExpressionContext)_localctx).memberName = this.name();
                    this.setState(68);
                    this.match(26);
                    this.setState(69);
                    ((SuperCallExpressionContext)_localctx).args = this.arguments();
                    this.setState(70);
                    this.match(27);
                    break;
                }
                case 12: {
                    _localctx = new StaticMethodCallExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(72);
                    ((StaticMethodCallExpressionContext)_localctx).memberName = this.name();
                    this.setState(73);
                    this.match(26);
                    this.setState(74);
                    ((StaticMethodCallExpressionContext)_localctx).args = this.arguments();
                    this.setState(75);
                    this.match(27);
                    break;
                }
                case 13: {
                    _localctx = new FreeMethodReferenceExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(77);
                    this.match(46);
                    this.setState(78);
                    ((FreeMethodReferenceExpressionContext)_localctx).memberName = this.name();
                    break;
                }
                case 14: {
                    _localctx = new ConstructorReferenceExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(79);
                    ((ConstructorReferenceExpressionContext)_localctx).type = this.name();
                    this.setState(80);
                    this.match(46);
                    this.setState(81);
                    this.match(5);
                    break;
                }
                case 15: {
                    _localctx = new UnaryExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(83);
                    ((UnaryExpressionContext)_localctx).op = this._input.LT(1);
                    _la = this._input.LA(1);
                    if (_la != 19 && _la != 23) {
                        ((UnaryExpressionContext)_localctx).op = this._errHandler.recoverInline(this);
                    } else {
                        if (this._input.LA(1) == -1) {
                            this.matchedEOF = true;
                        }
                        this._errHandler.reportMatch(this);
                        this.consume();
                    }
                    this.setState(84);
                    ((UnaryExpressionContext)_localctx).expr = this.expression(15);
                    break;
                }
                case 16: {
                    _localctx = new InstantiationExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(85);
                    this.match(5);
                    this.setState(86);
                    ((InstantiationExpressionContext)_localctx).type = this.name();
                    this.setState(87);
                    this.match(26);
                    this.setState(88);
                    ((InstantiationExpressionContext)_localctx).args = this.arguments();
                    this.setState(89);
                    this.match(27);
                    break;
                }
                case 17: {
                    _localctx = new ArrayLitExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(91);
                    this.match(5);
                    this.setState(92);
                    ((ArrayLitExpressionContext)_localctx).elementType = this.nameWithDims();
                    this.setState(93);
                    this.match(28);
                    this.setState(94);
                    this.match(29);
                    this.setState(95);
                    this.match(30);
                    this.setState(96);
                    ((ArrayLitExpressionContext)_localctx).values = this.nonEmptyArguments();
                    this.setState(97);
                    this.match(31);
                    break;
                }
                case 18: {
                    _localctx = new NewArrayExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(99);
                    this.match(5);
                    this.setState(100);
                    ((NewArrayExpressionContext)_localctx).innerType = this.name();
                    this.setState(105);
                    this._errHandler.sync(this);
                    _alt = 1;
                    do {
                        switch (_alt) {
                            case 1: {
                                this.setState(101);
                                this.match(28);
                                this.setState(102);
                                ((NewArrayExpressionContext)_localctx).expression = this.expression(0);
                                ((NewArrayExpressionContext)_localctx).dims.add(((NewArrayExpressionContext)_localctx).expression);
                                this.setState(103);
                                this.match(29);
                                break;
                            }
                            default: {
                                throw new NoViableAltException(this);
                            }
                        }
                        this.setState(107);
                        this._errHandler.sync(this);
                    } while ((_alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 3, this._ctx)) != 2 && _alt != 0);
                    this.setState(113);
                    this._errHandler.sync(this);
                    _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 4, this._ctx);
                    while (_alt != 2 && _alt != 0) {
                        if (_alt == 1) {
                            this.setState(109);
                            ((NewArrayExpressionContext)_localctx).LeftBracket = this.match(28);
                            ((NewArrayExpressionContext)_localctx).blankDims.add(((NewArrayExpressionContext)_localctx).LeftBracket);
                            this.setState(110);
                            this.match(29);
                        }
                        this.setState(115);
                        this._errHandler.sync(this);
                        _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 4, this._ctx);
                    }
                    break;
                }
                case 19: {
                    _localctx = new CastExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(116);
                    this.match(26);
                    this.setState(117);
                    ((CastExpressionContext)_localctx).type = this.nameWithDims();
                    this.setState(118);
                    this.match(27);
                    this.setState(119);
                    ((CastExpressionContext)_localctx).expr = this.expression(11);
                    break;
                }
                case 20: {
                    _localctx = new ParenthesizedExpressionContext(_localctx);
                    this._ctx = _localctx;
                    _prevctx = _localctx;
                    this.setState(121);
                    this.match(26);
                    this.setState(122);
                    ((ParenthesizedExpressionContext)_localctx).expr = this.expression(0);
                    this.setState(123);
                    this.match(27);
                }
            }
            this._ctx.stop = this._input.LT(-1);
            this.setState(174);
            this._errHandler.sync(this);
            _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 7, this._ctx);
            while (_alt != 2 && _alt != 0) {
                if (_alt == 1) {
                    if (this._parseListeners != null) {
                        this.triggerExitRuleEvent();
                    }
                    _prevctx = _localctx;
                    this.setState(172);
                    this._errHandler.sync(this);
                    switch (((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 6, this._ctx)) {
                        case 1: {
                            _localctx = new MultiplicativeExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((MultiplicativeExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(127);
                            if (!this.precpred(this._ctx, 10)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 10)");
                            }
                            this.setState(128);
                            ((MultiplicativeExpressionContext)_localctx).op = this._input.LT(1);
                            _la = this._input.LA(1);
                            if ((_la & 0xFFFFFFC0) != 0 || (1L << _la & 0x700000L) == 0L) {
                                ((MultiplicativeExpressionContext)_localctx).op = this._errHandler.recoverInline(this);
                            } else {
                                if (this._input.LA(1) == -1) {
                                    this.matchedEOF = true;
                                }
                                this._errHandler.reportMatch(this);
                                this.consume();
                            }
                            this.setState(129);
                            ((MultiplicativeExpressionContext)_localctx).right = this.expression(11);
                            break;
                        }
                        case 2: {
                            _localctx = new AdditiveExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((AdditiveExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(130);
                            if (!this.precpred(this._ctx, 9)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 9)");
                            }
                            this.setState(131);
                            ((AdditiveExpressionContext)_localctx).op = this._input.LT(1);
                            _la = this._input.LA(1);
                            if (_la != 18 && _la != 19) {
                                ((AdditiveExpressionContext)_localctx).op = this._errHandler.recoverInline(this);
                            } else {
                                if (this._input.LA(1) == -1) {
                                    this.matchedEOF = true;
                                }
                                this._errHandler.reportMatch(this);
                                this.consume();
                            }
                            this.setState(132);
                            ((AdditiveExpressionContext)_localctx).right = this.expression(10);
                            break;
                        }
                        case 3: {
                            _localctx = new ShiftExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((ShiftExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(133);
                            if (!this.precpred(this._ctx, 8)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 8)");
                            }
                            this.setState(134);
                            ((ShiftExpressionContext)_localctx).op = this._input.LT(1);
                            _la = this._input.LA(1);
                            if ((_la & 0xFFFFFFC0) != 0 || (1L << _la & 0xE00000000L) == 0L) {
                                ((ShiftExpressionContext)_localctx).op = this._errHandler.recoverInline(this);
                            } else {
                                if (this._input.LA(1) == -1) {
                                    this.matchedEOF = true;
                                }
                                this._errHandler.reportMatch(this);
                                this.consume();
                            }
                            this.setState(135);
                            ((ShiftExpressionContext)_localctx).right = this.expression(9);
                            break;
                        }
                        case 4: {
                            _localctx = new ComparisonExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((ComparisonExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(136);
                            if (!this.precpred(this._ctx, 7)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 7)");
                            }
                            this.setState(137);
                            ((ComparisonExpressionContext)_localctx).op = this._input.LT(1);
                            _la = this._input.LA(1);
                            if ((_la & 0xFFFFFFC0) != 0 || (1L << _la & 0xF000000000L) == 0L) {
                                ((ComparisonExpressionContext)_localctx).op = this._errHandler.recoverInline(this);
                            } else {
                                if (this._input.LA(1) == -1) {
                                    this.matchedEOF = true;
                                }
                                this._errHandler.reportMatch(this);
                                this.consume();
                            }
                            this.setState(138);
                            ((ComparisonExpressionContext)_localctx).right = this.expression(8);
                            break;
                        }
                        case 5: {
                            _localctx = new EqualityExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((EqualityExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(139);
                            if (!this.precpred(this._ctx, 5)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 5)");
                            }
                            this.setState(140);
                            ((EqualityExpressionContext)_localctx).op = this._input.LT(1);
                            _la = this._input.LA(1);
                            if (_la != 40 && _la != 41) {
                                ((EqualityExpressionContext)_localctx).op = this._errHandler.recoverInline(this);
                            } else {
                                if (this._input.LA(1) == -1) {
                                    this.matchedEOF = true;
                                }
                                this._errHandler.reportMatch(this);
                                this.consume();
                            }
                            this.setState(141);
                            ((EqualityExpressionContext)_localctx).right = this.expression(6);
                            break;
                        }
                        case 6: {
                            _localctx = new BitwiseAndExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((BitwiseAndExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(142);
                            if (!this.precpred(this._ctx, 4)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                            }
                            this.setState(143);
                            this.match(42);
                            this.setState(144);
                            ((BitwiseAndExpressionContext)_localctx).right = this.expression(5);
                            break;
                        }
                        case 7: {
                            _localctx = new BitwiseXorExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((BitwiseXorExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(145);
                            if (!this.precpred(this._ctx, 3)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                            }
                            this.setState(146);
                            this.match(43);
                            this.setState(147);
                            ((BitwiseXorExpressionContext)_localctx).right = this.expression(4);
                            break;
                        }
                        case 8: {
                            _localctx = new BitwiseOrExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((BitwiseOrExpressionContext)_localctx).left = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(148);
                            if (!this.precpred(this._ctx, 2)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                            }
                            this.setState(149);
                            this.match(44);
                            this.setState(150);
                            ((BitwiseOrExpressionContext)_localctx).right = this.expression(3);
                            break;
                        }
                        case 9: {
                            _localctx = new ArrayAccessExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((ArrayAccessExpressionContext)_localctx).arr = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(151);
                            if (!this.precpred(this._ctx, 23)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 23)");
                            }
                            this.setState(152);
                            this.match(28);
                            this.setState(153);
                            ((ArrayAccessExpressionContext)_localctx).index = this.expression(0);
                            this.setState(154);
                            this.match(29);
                            break;
                        }
                        case 10: {
                            _localctx = new MemberAccessExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((MemberAccessExpressionContext)_localctx).receiver = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(156);
                            if (!this.precpred(this._ctx, 22)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 22)");
                            }
                            this.setState(157);
                            this.match(24);
                            this.setState(158);
                            ((MemberAccessExpressionContext)_localctx).memberName = this.name();
                            break;
                        }
                        case 11: {
                            _localctx = new MethodCallExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((MethodCallExpressionContext)_localctx).receiver = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(159);
                            if (!this.precpred(this._ctx, 20)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 20)");
                            }
                            this.setState(160);
                            this.match(24);
                            this.setState(161);
                            ((MethodCallExpressionContext)_localctx).memberName = this.name();
                            this.setState(162);
                            this.match(26);
                            this.setState(163);
                            ((MethodCallExpressionContext)_localctx).args = this.arguments();
                            this.setState(164);
                            this.match(27);
                            break;
                        }
                        case 12: {
                            _localctx = new BoundMethodReferenceExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((BoundMethodReferenceExpressionContext)_localctx).receiver = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(166);
                            if (!this.precpred(this._ctx, 18)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 18)");
                            }
                            this.setState(167);
                            this.match(46);
                            this.setState(168);
                            ((BoundMethodReferenceExpressionContext)_localctx).memberName = this.name();
                            break;
                        }
                        case 13: {
                            _localctx = new InstanceofExpressionContext(new ExpressionContext(_parentctx, _parentState));
                            ((InstanceofExpressionContext)_localctx).expr = _prevctx;
                            this.pushNewRecursionContext(_localctx, _startState, 2);
                            this.setState(169);
                            if (!this.precpred(this._ctx, 6)) {
                                throw new FailedPredicateException(this, "precpred(_ctx, 6)");
                            }
                            this.setState(170);
                            this.match(6);
                            this.setState(171);
                            ((InstanceofExpressionContext)_localctx).type = this.nameWithDims();
                        }
                    }
                }
                this.setState(176);
                this._errHandler.sync(this);
                _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 7, this._ctx);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            this._errHandler.reportError(this, re);
            this._errHandler.recover(this, re);
        }
        finally {
            this.unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public final NameContext name() throws RecognitionException {
        NameContext _localctx = new NameContext(this._ctx, this.getState());
        this.enterRule(_localctx, 6, 3);
        try {
            this.setState(179);
            this._errHandler.sync(this);
            switch (this._input.LA(1)) {
                case 15: {
                    _localctx = new IdentifierNameContext(_localctx);
                    this.enterOuterAlt(_localctx, 1);
                    this.setState(177);
                    this.match(15);
                    return _localctx;
                }
                case 4: {
                    _localctx = new WildcardNameContext(_localctx);
                    this.enterOuterAlt(_localctx, 2);
                    this.setState(178);
                    this.match(4);
                    return _localctx;
                }
                default: {
                    throw new NoViableAltException(this);
                }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            this._errHandler.reportError(this, re);
            this._errHandler.recover(this, re);
            return _localctx;
        }
        finally {
            this.exitRule();
        }
    }

    public final NameWithDimsContext nameWithDims() throws RecognitionException {
        NameWithDimsContext _localctx = new NameWithDimsContext(this._ctx, this.getState());
        this.enterRule(_localctx, 8, 4);
        try {
            this.enterOuterAlt(_localctx, 1);
            this.setState(181);
            this.name();
            this.setState(186);
            this._errHandler.sync(this);
            int _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 9, this._ctx);
            while (_alt != 2 && _alt != 0) {
                if (_alt == 1) {
                    this.setState(182);
                    _localctx.LeftBracket = this.match(28);
                    _localctx.dims.add(_localctx.LeftBracket);
                    this.setState(183);
                    this.match(29);
                }
                this.setState(188);
                this._errHandler.sync(this);
                _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 9, this._ctx);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            this._errHandler.reportError(this, re);
            this._errHandler.recover(this, re);
        }
        finally {
            this.exitRule();
        }
        return _localctx;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public final ArgumentsContext arguments() throws RecognitionException {
        ArgumentsContext _localctx = new ArgumentsContext(this._ctx, this.getState());
        this.enterRule(_localctx, 10, 5);
        try {
            this.enterOuterAlt(_localctx, 1);
            this.setState(190);
            this._errHandler.sync(this);
            int _la = this._input.LA(1);
            if ((_la & 0xFFFFFFC0) == 0 && (1L << _la & 0x4001048B99B8L) != 0L) {
                this.setState(189);
                this.nonEmptyArguments();
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            this._errHandler.reportError(this, re);
            this._errHandler.recover(this, re);
        }
        finally {
            this.exitRule();
        }
        return _localctx;
    }

    public final NonEmptyArgumentsContext nonEmptyArguments() throws RecognitionException {
        NonEmptyArgumentsContext _localctx = new NonEmptyArgumentsContext(this._ctx, this.getState());
        this.enterRule(_localctx, 12, 6);
        try {
            this.enterOuterAlt(_localctx, 1);
            this.setState(197);
            this._errHandler.sync(this);
            int _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 11, this._ctx);
            while (_alt != 2 && _alt != 0) {
                if (_alt == 1) {
                    this.setState(192);
                    this.expression(0);
                    this.setState(193);
                    this.match(25);
                }
                this.setState(199);
                this._errHandler.sync(this);
                _alt = ((ParserATNSimulator)this.getInterpreter()).adaptivePredict(this._input, 11, this._ctx);
            }
            this.setState(200);
            this.expression(0);
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            this._errHandler.reportError(this, re);
            this._errHandler.recover(this, re);
        }
        finally {
            this.exitRule();
        }
        return _localctx;
    }

    @Override
    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 2: {
                return this.expression_sempred((ExpressionContext)_localctx, predIndex);
            }
        }
        return true;
    }

    private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0: {
                return this.precpred(this._ctx, 10);
            }
            case 1: {
                return this.precpred(this._ctx, 9);
            }
            case 2: {
                return this.precpred(this._ctx, 8);
            }
            case 3: {
                return this.precpred(this._ctx, 7);
            }
            case 4: {
                return this.precpred(this._ctx, 5);
            }
            case 5: {
                return this.precpred(this._ctx, 4);
            }
            case 6: {
                return this.precpred(this._ctx, 3);
            }
            case 7: {
                return this.precpred(this._ctx, 2);
            }
            case 8: {
                return this.precpred(this._ctx, 23);
            }
            case 9: {
                return this.precpred(this._ctx, 22);
            }
            case 10: {
                return this.precpred(this._ctx, 20);
            }
            case 11: {
                return this.precpred(this._ctx, 18);
            }
            case 12: {
                return this.precpred(this._ctx, 6);
            }
        }
        return true;
    }

    static {
        int i;
        RuntimeMetaData.checkVersion("4.13.1", "4.13.1");
        _sharedContextCache = new PredictionContextCache();
        ruleNames = ExpressionParser.makeRuleNames();
        _LITERAL_NAMES = ExpressionParser.makeLiteralNames();
        _SYMBOLIC_NAMES = ExpressionParser.makeSymbolicNames();
        VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (i = 0; i < tokenNames.length; ++i) {
            ExpressionParser.tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                ExpressionParser.tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }
            if (tokenNames[i] != null) continue;
            ExpressionParser.tokenNames[i] = "<INVALID>";
        }
        _ATN = new ATNDeserializer().deserialize("\u0004\u00010\u00cb\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001(\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u00023\b\u0002\u0001\u0002\u0001\u0002\u0003\u00027\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0004\u0002j\b\u0002\u000b\u0002\f\u0002k\u0001\u0002\u0001\u0002\u0005\u0002p\b\u0002\n\u0002\f\u0002s\t\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002~\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002\u00ad\b\u0002\n\u0002\f\u0002\u00b0\t\u0002\u0001\u0003\u0001\u0003\u0003\u0003\u00b4\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004\u00b9\b\u0004\n\u0004\f\u0004\u00bc\t\u0004\u0001\u0005\u0003\u0005\u00bf\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006\u00c4\b\u0006\n\u0006\f\u0006\u00c7\t\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0000\u0001\u0004\u0007\u0000\u0002\u0004\u0006\b\n\f\u0000\u0006\u0002\u0000\u0013\u0013\u0017\u0017\u0001\u0000\u0014\u0016\u0001\u0000\u0012\u0013\u0001\u0000!#\u0001\u0000$'\u0001\u0000()\u00f0\u0000\u000e\u0001\u0000\u0000\u0000\u0002'\u0001\u0000\u0000\u0000\u0004}\u0001\u0000\u0000\u0000\u0006\u00b3\u0001\u0000\u0000\u0000\b\u00b5\u0001\u0000\u0000\u0000\n\u00be\u0001\u0000\u0000\u0000\f\u00c5\u0001\u0000\u0000\u0000\u000e\u000f\u0003\u0002\u0001\u0000\u000f\u0010\u0005\u0000\u0000\u0001\u0010\u0001\u0001\u0000\u0000\u0000\u0011\u0012\u0003\u0004\u0002\u0000\u0012\u0013\u0005\u0018\u0000\u0000\u0013\u0014\u0003\u0006\u0003\u0000\u0014\u0015\u0005-\u0000\u0000\u0015\u0016\u0003\u0004\u0002\u0000\u0016(\u0001\u0000\u0000\u0000\u0017\u0018\u0003\u0004\u0002\u0000\u0018\u0019\u0005\u001c\u0000\u0000\u0019\u001a\u0003\u0004\u0002\u0000\u001a\u001b\u0005\u001d\u0000\u0000\u001b\u001c\u0005-\u0000\u0000\u001c\u001d\u0003\u0004\u0002\u0000\u001d(\u0001\u0000\u0000\u0000\u001e\u001f\u0003\u0006\u0003\u0000\u001f \u0005-\u0000\u0000 !\u0003\u0004\u0002\u0000!(\u0001\u0000\u0000\u0000\"#\u0005\t\u0000\u0000#(\u0003\u0004\u0002\u0000$%\u0005\n\u0000\u0000%(\u0003\u0004\u0002\u0000&(\u0003\u0004\u0002\u0000'\u0011\u0001\u0000\u0000\u0000'\u0017\u0001\u0000\u0000\u0000'\u001e\u0001\u0000\u0000\u0000'\"\u0001\u0000\u0000\u0000'$\u0001\u0000\u0000\u0000'&\u0001\u0000\u0000\u0000(\u0003\u0001\u0000\u0000\u0000)*\u0006\u0002\uffff\uffff\u0000*+\u0005 \u0000\u0000+,\u0005\u001a\u0000\u0000,-\u0003\u0004\u0002\u0000-.\u0005\u001b\u0000\u0000.~\u0001\u0000\u0000\u0000/~\u0005\u0004\u0000\u00000~\u0005\u000b\u0000\u000013\u0005\u0013\u0000\u000021\u0001\u0000\u0000\u000023\u0001\u0000\u0000\u000034\u0001\u0000\u0000\u00004~\u0005\u0010\u0000\u000057\u0005\u0013\u0000\u000065\u0001\u0000\u0000\u000067\u0001\u0000\u0000\u000078\u0001\u0000\u0000\u00008~\u0005\u0011\u0000\u00009~\u0005\u0007\u0000\u0000:~\u0005\b\u0000\u0000;~\u0005\u0003\u0000\u0000<~\u0005\u000f\u0000\u0000=>\u0003\b\u0004\u0000>?\u0005\u0018\u0000\u0000?@\u0005\r\u0000\u0000@~\u0001\u0000\u0000\u0000AB\u0005\f\u0000\u0000BC\u0005\u0018\u0000\u0000CD\u0003\u0006\u0003\u0000DE\u0005\u001a\u0000\u0000EF\u0003\n\u0005\u0000FG\u0005\u001b\u0000\u0000G~\u0001\u0000\u0000\u0000HI\u0003\u0006\u0003\u0000IJ\u0005\u001a\u0000\u0000JK\u0003\n\u0005\u0000KL\u0005\u001b\u0000\u0000L~\u0001\u0000\u0000\u0000MN\u0005.\u0000\u0000N~\u0003\u0006\u0003\u0000OP\u0003\u0006\u0003\u0000PQ\u0005.\u0000\u0000QR\u0005\u0005\u0000\u0000R~\u0001\u0000\u0000\u0000ST\u0007\u0000\u0000\u0000T~\u0003\u0004\u0002\u000fUV\u0005\u0005\u0000\u0000VW\u0003\u0006\u0003\u0000WX\u0005\u001a\u0000\u0000XY\u0003\n\u0005\u0000YZ\u0005\u001b\u0000\u0000Z~\u0001\u0000\u0000\u0000[\\\u0005\u0005\u0000\u0000\\]\u0003\b\u0004\u0000]^\u0005\u001c\u0000\u0000^_\u0005\u001d\u0000\u0000_`\u0005\u001e\u0000\u0000`a\u0003\f\u0006\u0000ab\u0005\u001f\u0000\u0000b~\u0001\u0000\u0000\u0000cd\u0005\u0005\u0000\u0000di\u0003\u0006\u0003\u0000ef\u0005\u001c\u0000\u0000fg\u0003\u0004\u0002\u0000gh\u0005\u001d\u0000\u0000hj\u0001\u0000\u0000\u0000ie\u0001\u0000\u0000\u0000jk\u0001\u0000\u0000\u0000ki\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000\u0000lq\u0001\u0000\u0000\u0000mn\u0005\u001c\u0000\u0000np\u0005\u001d\u0000\u0000om\u0001\u0000\u0000\u0000ps\u0001\u0000\u0000\u0000qo\u0001\u0000\u0000\u0000qr\u0001\u0000\u0000\u0000r~\u0001\u0000\u0000\u0000sq\u0001\u0000\u0000\u0000tu\u0005\u001a\u0000\u0000uv\u0003\b\u0004\u0000vw\u0005\u001b\u0000\u0000wx\u0003\u0004\u0002\u000bx~\u0001\u0000\u0000\u0000yz\u0005\u001a\u0000\u0000z{\u0003\u0004\u0002\u0000{|\u0005\u001b\u0000\u0000|~\u0001\u0000\u0000\u0000})\u0001\u0000\u0000\u0000}/\u0001\u0000\u0000\u0000}0\u0001\u0000\u0000\u0000}2\u0001\u0000\u0000\u0000}6\u0001\u0000\u0000\u0000}9\u0001\u0000\u0000\u0000}:\u0001\u0000\u0000\u0000};\u0001\u0000\u0000\u0000}<\u0001\u0000\u0000\u0000}=\u0001\u0000\u0000\u0000}A\u0001\u0000\u0000\u0000}H\u0001\u0000\u0000\u0000}M\u0001\u0000\u0000\u0000}O\u0001\u0000\u0000\u0000}S\u0001\u0000\u0000\u0000}U\u0001\u0000\u0000\u0000}[\u0001\u0000\u0000\u0000}c\u0001\u0000\u0000\u0000}t\u0001\u0000\u0000\u0000}y\u0001\u0000\u0000\u0000~\u00ae\u0001\u0000\u0000\u0000\u007f\u0080\n\n\u0000\u0000\u0080\u0081\u0007\u0001\u0000\u0000\u0081\u00ad\u0003\u0004\u0002\u000b\u0082\u0083\n\t\u0000\u0000\u0083\u0084\u0007\u0002\u0000\u0000\u0084\u00ad\u0003\u0004\u0002\n\u0085\u0086\n\b\u0000\u0000\u0086\u0087\u0007\u0003\u0000\u0000\u0087\u00ad\u0003\u0004\u0002\t\u0088\u0089\n\u0007\u0000\u0000\u0089\u008a\u0007\u0004\u0000\u0000\u008a\u00ad\u0003\u0004\u0002\b\u008b\u008c\n\u0005\u0000\u0000\u008c\u008d\u0007\u0005\u0000\u0000\u008d\u00ad\u0003\u0004\u0002\u0006\u008e\u008f\n\u0004\u0000\u0000\u008f\u0090\u0005*\u0000\u0000\u0090\u00ad\u0003\u0004\u0002\u0005\u0091\u0092\n\u0003\u0000\u0000\u0092\u0093\u0005+\u0000\u0000\u0093\u00ad\u0003\u0004\u0002\u0004\u0094\u0095\n\u0002\u0000\u0000\u0095\u0096\u0005,\u0000\u0000\u0096\u00ad\u0003\u0004\u0002\u0003\u0097\u0098\n\u0017\u0000\u0000\u0098\u0099\u0005\u001c\u0000\u0000\u0099\u009a\u0003\u0004\u0002\u0000\u009a\u009b\u0005\u001d\u0000\u0000\u009b\u00ad\u0001\u0000\u0000\u0000\u009c\u009d\n\u0016\u0000\u0000\u009d\u009e\u0005\u0018\u0000\u0000\u009e\u00ad\u0003\u0006\u0003\u0000\u009f\u00a0\n\u0014\u0000\u0000\u00a0\u00a1\u0005\u0018\u0000\u0000\u00a1\u00a2\u0003\u0006\u0003\u0000\u00a2\u00a3\u0005\u001a\u0000\u0000\u00a3\u00a4\u0003\n\u0005\u0000\u00a4\u00a5\u0005\u001b\u0000\u0000\u00a5\u00ad\u0001\u0000\u0000\u0000\u00a6\u00a7\n\u0012\u0000\u0000\u00a7\u00a8\u0005.\u0000\u0000\u00a8\u00ad\u0003\u0006\u0003\u0000\u00a9\u00aa\n\u0006\u0000\u0000\u00aa\u00ab\u0005\u0006\u0000\u0000\u00ab\u00ad\u0003\b\u0004\u0000\u00ac\u007f\u0001\u0000\u0000\u0000\u00ac\u0082\u0001\u0000\u0000\u0000\u00ac\u0085\u0001\u0000\u0000\u0000\u00ac\u0088\u0001\u0000\u0000\u0000\u00ac\u008b\u0001\u0000\u0000\u0000\u00ac\u008e\u0001\u0000\u0000\u0000\u00ac\u0091\u0001\u0000\u0000\u0000\u00ac\u0094\u0001\u0000\u0000\u0000\u00ac\u0097\u0001\u0000\u0000\u0000\u00ac\u009c\u0001\u0000\u0000\u0000\u00ac\u009f\u0001\u0000\u0000\u0000\u00ac\u00a6\u0001\u0000\u0000\u0000\u00ac\u00a9\u0001\u0000\u0000\u0000\u00ad\u00b0\u0001\u0000\u0000\u0000\u00ae\u00ac\u0001\u0000\u0000\u0000\u00ae\u00af\u0001\u0000\u0000\u0000\u00af\u0005\u0001\u0000\u0000\u0000\u00b0\u00ae\u0001\u0000\u0000\u0000\u00b1\u00b4\u0005\u000f\u0000\u0000\u00b2\u00b4\u0005\u0004\u0000\u0000\u00b3\u00b1\u0001\u0000\u0000\u0000\u00b3\u00b2\u0001\u0000\u0000\u0000\u00b4\u0007\u0001\u0000\u0000\u0000\u00b5\u00ba\u0003\u0006\u0003\u0000\u00b6\u00b7\u0005\u001c\u0000\u0000\u00b7\u00b9\u0005\u001d\u0000\u0000\u00b8\u00b6\u0001\u0000\u0000\u0000\u00b9\u00bc\u0001\u0000\u0000\u0000\u00ba\u00b8\u0001\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000\u00bb\t\u0001\u0000\u0000\u0000\u00bc\u00ba\u0001\u0000\u0000\u0000\u00bd\u00bf\u0003\f\u0006\u0000\u00be\u00bd\u0001\u0000\u0000\u0000\u00be\u00bf\u0001\u0000\u0000\u0000\u00bf\u000b\u0001\u0000\u0000\u0000\u00c0\u00c1\u0003\u0004\u0002\u0000\u00c1\u00c2\u0005\u0019\u0000\u0000\u00c2\u00c4\u0001\u0000\u0000\u0000\u00c3\u00c0\u0001\u0000\u0000\u0000\u00c4\u00c7\u0001\u0000\u0000\u0000\u00c5\u00c3\u0001\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000\u0000\u0000\u00c6\u00c8\u0001\u0000\u0000\u0000\u00c7\u00c5\u0001\u0000\u0000\u0000\u00c8\u00c9\u0003\u0004\u0002\u0000\u00c9\r\u0001\u0000\u0000\u0000\f'26kq}\u00ac\u00ae\u00b3\u00ba\u00be\u00c5".toCharArray());
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (i = 0; i < _ATN.getNumberOfDecisions(); ++i) {
            ExpressionParser._decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }

    public static class RootContext
    extends ParserRuleContext {
        public StatementContext statement() {
            return this.getRuleContext(StatementContext.class, 0);
        }

        public RootContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return 0;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterRoot(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitRoot(this);
            }
        }
    }

    public static class StatementContext
    extends ParserRuleContext {
        public StatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return 1;
        }

        public StatementContext() {
        }

        public void copyFrom(StatementContext ctx) {
            super.copyFrom(ctx);
        }
    }

    public static class MemberAssignmentStatementContext
    extends StatementContext {
        public ExpressionContext receiver;
        public NameContext memberName;
        public ExpressionContext value;

        public MemberAssignmentStatementContext(StatementContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterMemberAssignmentStatement(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitMemberAssignmentStatement(this);
            }
        }
    }

    public static class ExpressionContext
    extends ParserRuleContext {
        public ExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return 2;
        }

        public ExpressionContext() {
        }

        public void copyFrom(ExpressionContext ctx) {
            super.copyFrom(ctx);
        }
    }

    public static class NameContext
    extends ParserRuleContext {
        public NameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return 3;
        }

        public NameContext() {
        }

        public void copyFrom(NameContext ctx) {
            super.copyFrom(ctx);
        }
    }

    public static class ArrayStoreStatementContext
    extends StatementContext {
        public ExpressionContext arr;
        public ExpressionContext index;
        public ExpressionContext value;

        public ArrayStoreStatementContext(StatementContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterArrayStoreStatement(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitArrayStoreStatement(this);
            }
        }
    }

    public static class IdentifierAssignmentStatementContext
    extends StatementContext {
        public NameContext identifier;
        public ExpressionContext value;

        public IdentifierAssignmentStatementContext(StatementContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterIdentifierAssignmentStatement(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitIdentifierAssignmentStatement(this);
            }
        }
    }

    public static class ReturnStatementContext
    extends StatementContext {
        public ExpressionContext value;

        public ReturnStatementContext(StatementContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterReturnStatement(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitReturnStatement(this);
            }
        }
    }

    public static class ThrowStatementContext
    extends StatementContext {
        public ExpressionContext value;

        public ThrowStatementContext(StatementContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterThrowStatement(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitThrowStatement(this);
            }
        }
    }

    public static class ExpressionStatementContext
    extends StatementContext {
        public ExpressionContext expression() {
            return this.getRuleContext(ExpressionContext.class, 0);
        }

        public ExpressionStatementContext(StatementContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterExpressionStatement(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitExpressionStatement(this);
            }
        }
    }

    public static class CapturingExpressionContext
    extends ExpressionContext {
        public ExpressionContext expr;

        public CapturingExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterCapturingExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitCapturingExpression(this);
            }
        }
    }

    public static class WildcardExpressionContext
    extends ExpressionContext {
        public WildcardExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterWildcardExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitWildcardExpression(this);
            }
        }
    }

    public static class ThisExpressionContext
    extends ExpressionContext {
        public ThisExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterThisExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitThisExpression(this);
            }
        }
    }

    public static class IntLitExpressionContext
    extends ExpressionContext {
        public Token lit;

        public IntLitExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterIntLitExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitIntLitExpression(this);
            }
        }
    }

    public static class DecimalLitExpressionContext
    extends ExpressionContext {
        public Token lit;

        public DecimalLitExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterDecimalLitExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitDecimalLitExpression(this);
            }
        }
    }

    public static class BoolLitExpressionContext
    extends ExpressionContext {
        public Token lit;

        public BoolLitExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterBoolLitExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitBoolLitExpression(this);
            }
        }
    }

    public static class NullExpressionContext
    extends ExpressionContext {
        public Token lit;

        public NullExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterNullExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitNullExpression(this);
            }
        }
    }

    public static class StringLitExpressionContext
    extends ExpressionContext {
        public Token lit;

        public StringLitExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterStringLitExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitStringLitExpression(this);
            }
        }
    }

    public static class IdentifierExpressionContext
    extends ExpressionContext {
        public Token id;

        public IdentifierExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterIdentifierExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitIdentifierExpression(this);
            }
        }
    }

    public static class ClassConstantExpressionContext
    extends ExpressionContext {
        public NameWithDimsContext type;

        public ClassConstantExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterClassConstantExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitClassConstantExpression(this);
            }
        }
    }

    public static class NameWithDimsContext
    extends ParserRuleContext {
        public Token LeftBracket;
        public List<Token> dims = new ArrayList<Token>();

        public NameContext name() {
            return this.getRuleContext(NameContext.class, 0);
        }

        public NameWithDimsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return 4;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterNameWithDims(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitNameWithDims(this);
            }
        }
    }

    public static class SuperCallExpressionContext
    extends ExpressionContext {
        public NameContext memberName;
        public ArgumentsContext args;

        public SuperCallExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterSuperCallExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitSuperCallExpression(this);
            }
        }
    }

    public static class ArgumentsContext
    extends ParserRuleContext {
        public NonEmptyArgumentsContext nonEmptyArguments() {
            return this.getRuleContext(NonEmptyArgumentsContext.class, 0);
        }

        public ArgumentsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return 5;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterArguments(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitArguments(this);
            }
        }
    }

    public static class StaticMethodCallExpressionContext
    extends ExpressionContext {
        public NameContext memberName;
        public ArgumentsContext args;

        public StaticMethodCallExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterStaticMethodCallExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitStaticMethodCallExpression(this);
            }
        }
    }

    public static class FreeMethodReferenceExpressionContext
    extends ExpressionContext {
        public NameContext memberName;

        public FreeMethodReferenceExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterFreeMethodReferenceExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitFreeMethodReferenceExpression(this);
            }
        }
    }

    public static class ConstructorReferenceExpressionContext
    extends ExpressionContext {
        public NameContext type;

        public ConstructorReferenceExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterConstructorReferenceExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitConstructorReferenceExpression(this);
            }
        }
    }

    public static class UnaryExpressionContext
    extends ExpressionContext {
        public Token op;
        public ExpressionContext expr;

        public UnaryExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterUnaryExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitUnaryExpression(this);
            }
        }
    }

    public static class InstantiationExpressionContext
    extends ExpressionContext {
        public NameContext type;
        public ArgumentsContext args;

        public InstantiationExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterInstantiationExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitInstantiationExpression(this);
            }
        }
    }

    public static class ArrayLitExpressionContext
    extends ExpressionContext {
        public NameWithDimsContext elementType;
        public NonEmptyArgumentsContext values;

        public ArrayLitExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterArrayLitExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitArrayLitExpression(this);
            }
        }
    }

    public static class NonEmptyArgumentsContext
    extends ParserRuleContext {
        public List<ExpressionContext> expression() {
            return this.getRuleContexts(ExpressionContext.class);
        }

        public NonEmptyArgumentsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return 6;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterNonEmptyArguments(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitNonEmptyArguments(this);
            }
        }
    }

    public static class NewArrayExpressionContext
    extends ExpressionContext {
        public NameContext innerType;
        public ExpressionContext expression;
        public List<ExpressionContext> dims = new ArrayList<ExpressionContext>();
        public Token LeftBracket;
        public List<Token> blankDims = new ArrayList<Token>();

        public NewArrayExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterNewArrayExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitNewArrayExpression(this);
            }
        }
    }

    public static class CastExpressionContext
    extends ExpressionContext {
        public NameWithDimsContext type;
        public ExpressionContext expr;

        public CastExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterCastExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitCastExpression(this);
            }
        }
    }

    public static class ParenthesizedExpressionContext
    extends ExpressionContext {
        public ExpressionContext expr;

        public ParenthesizedExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterParenthesizedExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitParenthesizedExpression(this);
            }
        }
    }

    public static class MultiplicativeExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public Token op;
        public ExpressionContext right;

        public MultiplicativeExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterMultiplicativeExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitMultiplicativeExpression(this);
            }
        }
    }

    public static class AdditiveExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public Token op;
        public ExpressionContext right;

        public AdditiveExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterAdditiveExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitAdditiveExpression(this);
            }
        }
    }

    public static class ShiftExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public Token op;
        public ExpressionContext right;

        public ShiftExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterShiftExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitShiftExpression(this);
            }
        }
    }

    public static class ComparisonExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public Token op;
        public ExpressionContext right;

        public ComparisonExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterComparisonExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitComparisonExpression(this);
            }
        }
    }

    public static class EqualityExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public Token op;
        public ExpressionContext right;

        public EqualityExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterEqualityExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitEqualityExpression(this);
            }
        }
    }

    public static class BitwiseAndExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public ExpressionContext right;

        public BitwiseAndExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterBitwiseAndExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitBitwiseAndExpression(this);
            }
        }
    }

    public static class BitwiseXorExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public ExpressionContext right;

        public BitwiseXorExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterBitwiseXorExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitBitwiseXorExpression(this);
            }
        }
    }

    public static class BitwiseOrExpressionContext
    extends ExpressionContext {
        public ExpressionContext left;
        public ExpressionContext right;

        public BitwiseOrExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterBitwiseOrExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitBitwiseOrExpression(this);
            }
        }
    }

    public static class ArrayAccessExpressionContext
    extends ExpressionContext {
        public ExpressionContext arr;
        public ExpressionContext index;

        public ArrayAccessExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterArrayAccessExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitArrayAccessExpression(this);
            }
        }
    }

    public static class MemberAccessExpressionContext
    extends ExpressionContext {
        public ExpressionContext receiver;
        public NameContext memberName;

        public MemberAccessExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterMemberAccessExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitMemberAccessExpression(this);
            }
        }
    }

    public static class MethodCallExpressionContext
    extends ExpressionContext {
        public ExpressionContext receiver;
        public NameContext memberName;
        public ArgumentsContext args;

        public MethodCallExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterMethodCallExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitMethodCallExpression(this);
            }
        }
    }

    public static class BoundMethodReferenceExpressionContext
    extends ExpressionContext {
        public ExpressionContext receiver;
        public NameContext memberName;

        public BoundMethodReferenceExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterBoundMethodReferenceExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitBoundMethodReferenceExpression(this);
            }
        }
    }

    public static class InstanceofExpressionContext
    extends ExpressionContext {
        public ExpressionContext expr;
        public NameWithDimsContext type;

        public InstanceofExpressionContext(ExpressionContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterInstanceofExpression(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitInstanceofExpression(this);
            }
        }
    }

    public static class IdentifierNameContext
    extends NameContext {
        public IdentifierNameContext(NameContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterIdentifierName(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitIdentifierName(this);
            }
        }
    }

    public static class WildcardNameContext
    extends NameContext {
        public WildcardNameContext(NameContext ctx) {
            this.copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).enterWildcardName(this);
            }
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof ExpressionParserListener) {
                ((ExpressionParserListener)listener).exitWildcardName(this);
            }
        }
    }
}

