// Generated from com\github\jasync_sql_extensions\SqlLexer.g4 by ANTLR 4.7.1

package com.github.jasync_sql_extensions;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SqlLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMENT=1, QUOTED_TEXT=2, DOUBLE_QUOTED_TEXT=3, ESCAPED_TEXT=4, NAMED_PARAM=5, 
		POSITIONAL_PARAM=6, LITERAL=7;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"QUOTE", "ESCAPE", "ESCAPE_QUOTE", "DOUBLE_QUOTE", "COLON", "DOUBLE_COLON", 
		"QUESTION", "DOUBLE_QUESTION", "NAME", "JAVA_LETTER", "COMMENT", "QUOTED_TEXT", 
		"DOUBLE_QUOTED_TEXT", "ESCAPED_TEXT", "NAMED_PARAM", "POSITIONAL_PARAM", 
		"LITERAL"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "COMMENT", "QUOTED_TEXT", "DOUBLE_QUOTED_TEXT", "ESCAPED_TEXT", 
		"NAMED_PARAM", "POSITIONAL_PARAM", "LITERAL"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
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


	public SqlLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "SqlLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 4:
			return COLON_sempred((RuleContext)_localctx, predIndex);
		case 5:
			return DOUBLE_COLON_sempred((RuleContext)_localctx, predIndex);
		case 6:
			return QUESTION_sempred((RuleContext)_localctx, predIndex);
		case 7:
			return DOUBLE_QUESTION_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean COLON_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return _input.LA(2) != ':';
		}
		return true;
	}
	private boolean DOUBLE_COLON_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return _input.LA(2) == ':';
		}
		return true;
	}
	private boolean QUESTION_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return _input.LA(2) != '?';
		}
		return true;
	}
	private boolean DOUBLE_QUESTION_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return _input.LA(2) == '?';
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\tv\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3"+
		"\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\5\nA\n\n\3\13\3\13\3\13\3\13\5"+
		"\13G\n\13\3\f\3\f\3\f\3\f\7\fM\n\f\f\f\16\fP\13\f\3\f\3\f\3\f\3\r\3\r"+
		"\3\r\7\rX\n\r\f\r\16\r[\13\r\3\r\3\r\3\16\3\16\6\16a\n\16\r\16\16\16b"+
		"\3\16\3\16\3\17\3\17\3\17\3\20\3\20\6\20l\n\20\r\20\16\20m\3\21\3\21\3"+
		"\22\3\22\3\22\5\22u\n\22\3N\2\23\3\2\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23"+
		"\2\25\2\27\3\31\4\33\5\35\6\37\7!\b#\t\3\2\t\4\2\60\60\62;\6\2&&C\\aa"+
		"c|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01\3\2\udc02\ue001\3\2))\3\2$"+
		"$\2v\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2"+
		"\2!\3\2\2\2\2#\3\2\2\2\3%\3\2\2\2\5\'\3\2\2\2\7)\3\2\2\2\t,\3\2\2\2\13"+
		".\3\2\2\2\r\61\3\2\2\2\17\65\3\2\2\2\218\3\2\2\2\23@\3\2\2\2\25F\3\2\2"+
		"\2\27H\3\2\2\2\31T\3\2\2\2\33^\3\2\2\2\35f\3\2\2\2\37i\3\2\2\2!o\3\2\2"+
		"\2#t\3\2\2\2%&\7)\2\2&\4\3\2\2\2\'(\7^\2\2(\6\3\2\2\2)*\5\5\3\2*+\5\3"+
		"\2\2+\b\3\2\2\2,-\7$\2\2-\n\3\2\2\2./\6\6\2\2/\60\7<\2\2\60\f\3\2\2\2"+
		"\61\62\6\7\3\2\62\63\7<\2\2\63\64\7<\2\2\64\16\3\2\2\2\65\66\6\b\4\2\66"+
		"\67\7A\2\2\67\20\3\2\2\289\6\t\5\29:\7A\2\2:;\7A\2\2;\22\3\2\2\2<A\5\25"+
		"\13\2=A\t\2\2\2>?\7A\2\2?A\7\60\2\2@<\3\2\2\2@=\3\2\2\2@>\3\2\2\2A\24"+
		"\3\2\2\2BG\t\3\2\2CG\n\4\2\2DE\t\5\2\2EG\t\6\2\2FB\3\2\2\2FC\3\2\2\2F"+
		"D\3\2\2\2G\26\3\2\2\2HI\7\61\2\2IJ\7,\2\2JN\3\2\2\2KM\13\2\2\2LK\3\2\2"+
		"\2MP\3\2\2\2NO\3\2\2\2NL\3\2\2\2OQ\3\2\2\2PN\3\2\2\2QR\7,\2\2RS\7\61\2"+
		"\2S\30\3\2\2\2TY\5\3\2\2UX\5\7\4\2VX\n\7\2\2WU\3\2\2\2WV\3\2\2\2X[\3\2"+
		"\2\2YW\3\2\2\2YZ\3\2\2\2Z\\\3\2\2\2[Y\3\2\2\2\\]\5\3\2\2]\32\3\2\2\2^"+
		"`\5\t\5\2_a\n\b\2\2`_\3\2\2\2ab\3\2\2\2b`\3\2\2\2bc\3\2\2\2cd\3\2\2\2"+
		"de\5\t\5\2e\34\3\2\2\2fg\5\5\3\2gh\13\2\2\2h\36\3\2\2\2ik\5\13\6\2jl\5"+
		"\23\n\2kj\3\2\2\2lm\3\2\2\2mk\3\2\2\2mn\3\2\2\2n \3\2\2\2op\5\17\b\2p"+
		"\"\3\2\2\2qu\5\r\7\2ru\5\21\t\2su\13\2\2\2tq\3\2\2\2tr\3\2\2\2ts\3\2\2"+
		"\2u$\3\2\2\2\13\2@FNWYbmt\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}