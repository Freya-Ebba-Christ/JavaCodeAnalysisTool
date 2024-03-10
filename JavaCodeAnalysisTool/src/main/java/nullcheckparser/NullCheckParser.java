package nullcheckparser;
/*
 * Application.java
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Example: java AnnotationRemover MyJavaFile.java com.example.MyAnnotation
 *
 * @author Freya Ebba Christ 
 */

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class NullCheckParser {
    public static void main(String[] args) throws Exception {
        // Load and parse the Java source file
        FileInputStream in = new FileInputStream("path/to/YourJavaFile.java");
        CompilationUnit compilationUnit = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8))
                .parse(in)
                .getResult()
                .orElseThrow(() -> new RuntimeException("Unable to parse the file"));

        // Enable lexical preservation
        LexicalPreservingPrinter.setup(compilationUnit);

        // Process the file
        NullCheckVisitor nullCheckVisitor = new NullCheckVisitor();
        compilationUnit.accept(nullCheckVisitor, null);
        compilationUnit.accept(new MethodAnnotationVisitor(), nullCheckVisitor.getMethodsToAnnotate());

        // Save the modified compilation unit
        saveToFile(compilationUnit, "path/to/ModifiedYourJavaFile.java");
    }

    private static class NullCheckVisitor extends VoidVisitorAdapter<Void> {
        private final Set<MethodDeclaration> methodsToAnnotate = new HashSet<>();

        @Override
        public void visit(BinaryExpr n, Void arg) {
            super.visit(n, arg);
            if ((n.getOperator() == BinaryExpr.Operator.EQUALS || n.getOperator() == BinaryExpr.Operator.NOT_EQUALS)
                    && (n.getLeft().isNullLiteralExpr() || n.getRight().isNullLiteralExpr())) {
                n.addOrphanComment(new com.github.javaparser.ast.comments.LineComment("Null check present"));
                n.findAncestor(MethodDeclaration.class).ifPresent(methodsToAnnotate::add);
            }
        }

        public Set<MethodDeclaration> getMethodsToAnnotate() {
            return methodsToAnnotate;
        }
    }

    private static class MethodAnnotationVisitor extends ModifierVisitor<Set<MethodDeclaration>> {
        @Override
        public MethodDeclaration visit(MethodDeclaration n, Set<MethodDeclaration> arg) {
            super.visit(n, arg);
            if (arg.contains(n)) {
                n.addAnnotation("NullCheckPerformed");
            }
            return n;
        }
    }

    private static void saveToFile(CompilationUnit cu, String path) throws IOException {
        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.write(LexicalPreservingPrinter.print(cu));
        }
    }
}

