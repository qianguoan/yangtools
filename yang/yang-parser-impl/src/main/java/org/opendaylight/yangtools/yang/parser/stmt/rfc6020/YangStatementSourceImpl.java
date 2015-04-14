package org.opendaylight.yangtools.yang.parser.stmt.rfc6020;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.opendaylight.yangtools.antlrv4.code.gen.YangStatementLexer;
import org.opendaylight.yangtools.antlrv4.code.gen.YangStatementParser;
import org.opendaylight.yangtools.yang.model.api.meta.StatementSource;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangStatementParserListenerImpl;
import org.opendaylight.yangtools.yang.parser.spi.source.PrefixToModule;
import org.opendaylight.yangtools.yang.parser.spi.source.QNameToStatementDefinition;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementSourceReference;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * This class represents implementation of StatementStreamSource
 * in order to emit YANG statements using supplied StatementWriter
 *
 */

public class YangStatementSourceImpl implements StatementStreamSource {

    private YangStatementParserListenerImpl yangStatementModelParser;
    private YangStatementParser.StatementContext statementContext;
    private ParseTreeWalker walker;

    public YangStatementSourceImpl(String fileName) {
        try {
            statementContext = parseYangSource(loadFile(fileName));
            walker = new ParseTreeWalker();
            yangStatementModelParser = new YangStatementParserListenerImpl(REF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private StatementSourceReference REF = new StatementSourceReference() {

        @Override
        public StatementSource getStatementSource() {
            return StatementSource.DECLARATION;
        }
    };

    @Override
    public void writeLinkage(StatementWriter writer, QNameToStatementDefinition stmtDef) throws SourceException {
        yangStatementModelParser.setAttributes(writer, stmtDef);
        walker.walk(yangStatementModelParser, statementContext);
    }

    @Override
    public void writeLinkageAndStatementDefinitions(StatementWriter writer, QNameToStatementDefinition stmtDef, PrefixToModule prefixes) throws SourceException {
        yangStatementModelParser.setAttributes(writer, stmtDef, prefixes);
        walker.walk(yangStatementModelParser, statementContext);
    }

    @Override
    public void writeFull(StatementWriter writer, QNameToStatementDefinition stmtDef, PrefixToModule prefixes) throws SourceException {
        yangStatementModelParser.setAttributes(writer, stmtDef, prefixes);
        walker.walk(yangStatementModelParser, statementContext);
    }

    private FileInputStream loadFile(String fileName) throws Exception {
        return new FileInputStream(new File(getClass().getResource(fileName).toURI()));
    }

    private YangStatementParser.StatementContext parseYangSource(final InputStream stream) throws IOException, YangSyntaxErrorException {
        final YangStatementLexer lexer = new YangStatementLexer(new ANTLRInputStream(stream));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final YangStatementParser parser = new YangStatementParser(tokens);
        final YangStatementParser.StatementContext result = parser.statement();
        return result;
    }
}
