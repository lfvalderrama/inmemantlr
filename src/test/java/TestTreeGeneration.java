/**
 * Inmemantlr - In memory compiler for Antlr 4
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Julian Thome <julian.thome.de@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 **/

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.snt.inmemantlr.DefaultTreeListener;
import org.snt.inmemantlr.GenericParser;
import org.snt.inmemantlr.exceptions.IllegalWorkflowException;
import org.snt.inmemantlr.tree.Ast;
import org.snt.inmemantlr.tree.AstNode;
import org.snt.inmemantlr.utils.FileUtils;

import java.io.File;
import java.util.Set;

import static org.junit.Assert.*;

public class TestTreeGeneration {

    static File grammar = null;
    static File sfile = null;

    @Before
    public void init() {
        ClassLoader classLoader = getClass().getClassLoader();
        grammar = new File(classLoader.getResource("Java.g4").getFile());
        sfile = new File(classLoader.getResource("HelloWorld.java").getFile());
    }

    @Test
    public void testGeneration() {
        GenericParser gp = new GenericParser(grammar);
        gp.compile();
        String s = FileUtils.loadFileContent(sfile.getAbsolutePath());

        assertTrue(s != null && !s.isEmpty());

        DefaultTreeListener dlist = new DefaultTreeListener();

        gp.setListener(dlist);

        try {
            gp.parse(s);
        } catch (IllegalWorkflowException e) {
            assertTrue(false);
        }

        Ast ast = dlist.getAst();

        // create copy
        Ast cast = new Ast(ast);

        assertTrue(ast != null);
        Assert.assertEquals(ast.getNodes().size(), cast.getNodes().size());

        Set<Ast> asts = ast.getSubtrees(n -> "expression".equals(n.getRule()));

        assert asts.size() == 5;

        for (Ast a : asts) {
            assertTrue(ast.hasSubtree(a));
            assertFalse(ast.getSubtree(a) == null);
        }

        int sizeBefore = ast.getNodes().size();

        Ast first = asts.iterator().next();

        ast.removeSubtree(first);

        assertTrue(ast.getNodes().size() + first.getNodes().size() == sizeBefore);

        Ast repl = new Ast("replacement", "replacement");

        cast.replaceSubtree(first, repl);

        assertTrue(cast.getNodes().size() == ast.getNodes().size() + 1);
        assertTrue(cast.getDominatingSubtrees(n -> "classBody".equals(n.getRule())).size() == 1);
        assertTrue(cast.toDot() != null && !cast.toDot().isEmpty());

        AstNode root = cast.getRoot();

        assertTrue(root.hasChildren());
        assertFalse(root.hasParent());

        for (AstNode n : cast.getNodes()) {
            assertTrue(n.getLabel() != null);
            assertTrue(n.getRule() != null);
            for (int i = 0; i < n.getChildren().size(); i++) {
                if (i == 0)
                    assertTrue(n.getChild(i).equals(n.getFirstChild()));
                if (i == n.getChildren().size() - 1)
                    assertTrue(n.getChild(i).equals(n.getLastChild()));
            }
        }

        for (AstNode c : cast.getLeafs()) {
            assertTrue(c.hasParent());
            assertFalse(c.hasChildren());
            assertTrue(c.isLeaf());
            assertFalse(c.equals(null));
            assertFalse(c.equals(null));
            assertNull(c.getLastChild());
            assertNull(c.getFirstChild());
        }

        AstNode croot = cast.getRoot();
        croot.setParent(ast.getRoot());
        assertTrue(croot.getParent().equals(ast.getRoot()));
    }
}
