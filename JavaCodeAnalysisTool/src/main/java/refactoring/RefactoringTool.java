package refactoring;
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
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.FileInputStream;
import java.io.FileWriter;

public class RefactoringTool {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: RefactoringTool <sourceFilePath>");
            return;
        }

        JavaParser parser = new JavaParser(new ParserConfiguration());

        try (FileInputStream in = new FileInputStream(args[0])) {
            ParseResult<CompilationUnit> parseResult = parser.parse(in);

            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit compilationUnit = parseResult.getResult().get();

                // Create an instance of AdvancedControlFlowAnalyzer
                AdvancedControlFlowAnalyzer analyzer = new AdvancedControlFlowAnalyzer();

                // Visit the CompilationUnit with the analyzer
                analyzer.visit(compilationUnit, null);

                // Create an instance of NullCheckMethodVisitor
                NullCheckMethodVisitor nullCheckVisitor = new NullCheckMethodVisitor();

                // Visit the CompilationUnit with the nullCheckVisitor
                nullCheckVisitor.visit(compilationUnit, null);

                // Optionally save the modified original CompilationUnit if modifications were made
                try (FileWriter writer = new FileWriter("Refactored" + args[0])) {
                    writer.write(LexicalPreservingPrinter.print(compilationUnit));
                }
            } else {
                System.out.println("Failed to parse the source file.");
            }
        }
    }
}




